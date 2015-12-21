package qbt.manifest;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.MapStruct;
import misc1.commons.ds.MapStructBuilder;
import misc1.commons.ds.MapStructType;
import misc1.commons.merge.Merge;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.QbtHashUtils;
import qbt.QbtUtils;
import qbt.VcsVersionDigest;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public final class QbtManifest extends MapStruct<QbtManifest, QbtManifest.Builder, RepoTip, RepoManifest, RepoManifest.Builder> {
    private static final Pattern REPO_PATTERN = Pattern.compile("^([0-9a-zA-Z._]*),([0-9a-zA-Z._]*):([0-9a-f]{40})$");
    private static final Pattern OLD_PACKAGE_PATTERN = Pattern.compile("^    ([0-9a-zA-Z._]*):(.*)$");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^    ([0-9a-zA-Z._]*)$");
    private static final Pattern METADATA_PATTERN = Pattern.compile("^        Metadata:([^=]*)=(.*)$");
    private static final Pattern NORMAL_DEP_PATTERN = Pattern.compile("^        ([A-Za-z]*):([0-9a-zA-Z._]*),([0-9a-zA-Z._]*)$");
    private static final Pattern REPLACE_DEP_PATTERN = Pattern.compile("^        (?:R|Replace):([0-9a-zA-Z._]*),([0-9a-zA-Z._]*),([0-9a-zA-Z._]*)$");
    private static final Pattern VERIFY_DEP_PATTERN = Pattern.compile("^        (?:V|Verify):([0-9a-zA-Z._]*),([0-9a-zA-Z._]*),([0-9a-zA-Z._]*)$");

    public final ImmutableMap<RepoTip, RepoManifest> repos;
    public final ImmutableMap<PackageTip, RepoTip> packageToRepo;

    private QbtManifest(ImmutableMap<RepoTip, RepoManifest> repos) {
        super(TYPE, repos);

        this.repos = repos;

        ImmutableMap.Builder<PackageTip, RepoTip> packageToRepoBuilder = ImmutableMap.builder();
        for(Map.Entry<RepoTip, RepoManifest> e : repos.entrySet()) {
            for(String pkg : e.getValue().packages.keySet()) {
                packageToRepoBuilder.put(PackageTip.TYPE.of(pkg, e.getKey().tip), e.getKey());
            }
        }
        this.packageToRepo = packageToRepoBuilder.build();
    }

    public static class Builder extends MapStructBuilder<QbtManifest, Builder, RepoTip, RepoManifest, RepoManifest.Builder> {
        public Builder(ImmutableSalvagingMap<RepoTip, RepoManifest.Builder> map) {
            super(TYPE, map);
        }
    }

    public static QbtManifest parse(Path f) throws IOException {
        return parse(f.toAbsolutePath().toString(), QbtUtils.readLines(f));
    }

    private static final class Parser {
        private Builder b = TYPE.builder();
        private RepoTip currentRepo = null;
        private RepoManifest.Builder repoBuilder = null;
        private String currentPackage = null;
        private PackageManifest.Builder packageBuilder = null;

        private void closePackage() {
            if(packageBuilder == null) {
                return;
            }

            repoBuilder = repoBuilder.with(currentPackage, packageBuilder);
            currentPackage = null;
            packageBuilder = null;
        }

        private void closeRepo() {
            if(repoBuilder == null) {
                return;
            }
            closePackage();
            b = b.with(currentRepo, repoBuilder);
            currentRepo = null;
            repoBuilder = null;
        }

        public void line(String line) {
            Matcher repoMatcher = REPO_PATTERN.matcher(line);
            if(repoMatcher.matches()) {
                closeRepo();
                currentRepo = RepoTip.TYPE.of(repoMatcher.group(1), repoMatcher.group(2));
                repoBuilder = RepoManifest.TYPE.builder();
                repoBuilder = repoBuilder.set(RepoManifest.VERSION, new VcsVersionDigest(QbtHashUtils.parse(repoMatcher.group(3))));
                return;
            }
            else if(currentRepo == null) {
                throw new IllegalArgumentException("expecting repo identifier of the form '<repo-name>,<repo-tip>:<sha1>' but got '" + line + "'");
            }

            Matcher oldPackageMatcher = OLD_PACKAGE_PATTERN.matcher(line);
            Matcher packageMatcher = PACKAGE_PATTERN.matcher(line);
            if(oldPackageMatcher.matches()) {
                closePackage();
                currentPackage = oldPackageMatcher.group(1);
                packageBuilder = PackageManifest.TYPE.builder().withStringMetadata("prefix", oldPackageMatcher.group(2));
                return;
            }
            else if(packageMatcher.matches()) {
                closePackage();
                currentPackage = packageMatcher.group(1);
                packageBuilder = PackageManifest.TYPE.builder();
                return;
            }
            else if(currentPackage == null) {
                throw new IllegalArgumentException("expecting package identifier but got '" + line +"'");
            }

            Matcher metadataMatcher = METADATA_PATTERN.matcher(line);
            if(metadataMatcher.matches()) {
                packageBuilder = packageBuilder.withStringMetadata(metadataMatcher.group(1), metadataMatcher.group(2));
                return;
            }

            Matcher normalDepMatcher = NORMAL_DEP_PATTERN.matcher(line);
            if(normalDepMatcher.matches()) {
                packageBuilder = packageBuilder.withNormalDep(PackageTip.TYPE.of(normalDepMatcher.group(2), normalDepMatcher.group(3)), NormalDependencyType.fromTag(normalDepMatcher.group(1)));
                return;
            }

            Matcher replaceDepMatcher = REPLACE_DEP_PATTERN.matcher(line);
            if(replaceDepMatcher.matches()) {
                packageBuilder = packageBuilder.withReplaceDep(PackageTip.TYPE.of(replaceDepMatcher.group(1), replaceDepMatcher.group(2)), replaceDepMatcher.group(3));
                return;
            }

            Matcher verifyDepMatcher = VERIFY_DEP_PATTERN.matcher(line);
            if(verifyDepMatcher.matches()) {
                packageBuilder = packageBuilder.withVerifyDep(PackageTip.TYPE.of(verifyDepMatcher.group(1), verifyDepMatcher.group(2)), verifyDepMatcher.group(3));
                return;
            }

            throw new IllegalArgumentException("expecting dependency or metadata line but got '" + line + "'");
        }

        public QbtManifest complete() {
            closeRepo();
            return b.build();
        }
    }

    public static QbtManifest parse(String desc, Iterable<String> lines) {
        Parser p = new Parser();
        for(String line : lines) {
            p.line(line);
        }
        return p.complete();
    }

    private static interface SimpleDeparseBuilder {
        public void add(String line);
    }

    private static class PoolSimpleDeparseBuilder implements SimpleDeparseBuilder {
        private final ImmutableList.Builder<String> b = ImmutableList.builder();

        @Override
        public void add(String line) {
            b.add(line);
        }

        public ImmutableList<String> build() {
            return b.build();
        }
    }

    private static interface ConflictDeparseBuilder extends SimpleDeparseBuilder {
        public void addConflict(String path, String type, Iterable<String> lhs, Iterable<String> mhs, Iterable<String> rhs);
    }

    private static abstract class Deparser<C, V> {
        private void deparseCompleteConflict(ConflictDeparseBuilder b, String path, C context, String type, V lhs, V mhs, V rhs) {
            PoolSimpleDeparseBuilder lhsLines = new PoolSimpleDeparseBuilder();
            if(lhs != null) {
                deparseSimple(lhsLines, context, lhs);
            }
            PoolSimpleDeparseBuilder mhsLines = new PoolSimpleDeparseBuilder();
            if(mhs != null) {
                deparseSimple(mhsLines, context, mhs);
            }
            PoolSimpleDeparseBuilder rhsLines = new PoolSimpleDeparseBuilder();
            if(rhs != null) {
                deparseSimple(rhsLines, context, rhs);
            }
            b.addConflict(path, type, lhsLines.build(), mhsLines.build(), rhsLines.build());
        }

        public final void deparse(ConflictDeparseBuilder b, String path, C context, V lhs, V mhs, V rhs) {
            if(Objects.equal(mhs, lhs)) {
                deparseSimple(b, context, rhs);
                return;
            }
            if(Objects.equal(mhs, rhs)) {
                deparseSimple(b, context, lhs);
                return;
            }
            if(Objects.equal(lhs, rhs)) {
                deparseSimple(b, context, lhs);
                return;
            }
            if(lhs == null) {
                deparseCompleteConflict(b, path, context, "DELETE/EDIT", lhs, mhs, rhs);
                return;
            }
            if(mhs == null) {
                deparseCompleteConflict(b, path, context, "ADD/ADD", lhs, mhs, rhs);
                return;
            }
            if(rhs == null) {
                deparseCompleteConflict(b, path, context, "EDIT/DELETE", lhs, mhs, rhs);
                return;
            }
            deparseConflict(b, path, context, lhs, mhs, rhs);
        }

        protected abstract void deparseSimple(SimpleDeparseBuilder b, C context, V v);
        protected abstract void deparseConflict(ConflictDeparseBuilder b, String path, C context, V lhs, V mhs, V rhs);
    }

    private static abstract class ConflictMarkerDeparser<C, V> extends Deparser<C, V> {
        @Override
        protected void deparseConflict(ConflictDeparseBuilder b, String path, C context, V lhs, V mhs, V rhs) {
            PoolSimpleDeparseBuilder lhsLines = new PoolSimpleDeparseBuilder();
            deparseSimple(lhsLines, context, lhs);
            PoolSimpleDeparseBuilder mhsLines = new PoolSimpleDeparseBuilder();
            deparseSimple(mhsLines, context, mhs);
            PoolSimpleDeparseBuilder rhsLines = new PoolSimpleDeparseBuilder();
            deparseSimple(rhsLines, context, rhs);
            b.addConflict(path, "EDIT/EDIT", lhsLines.build(), mhsLines.build(), rhsLines.build());
        }
    }

    private static String addPath(String p1, String p2) {
        if(p1.isEmpty()) {
            return p2;
        }
        if(p2.isEmpty()) {
            return p1;
        }
        return p1 + "/" + p2;
    }

    private static class MapDeparser<C, K, V> extends Deparser<C, Map<K, V>> {
        private final Comparator<K> comparator;
        private final Deparser<K, V> entryDeparser;

        public MapDeparser(Comparator<K> comparator, Deparser<K, V> entryDeparser) {
            this.comparator = comparator;
            this.entryDeparser = entryDeparser;
        }

        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, C context, Map<K, V> map) {
            Set<K> ks = Sets.newTreeSet(comparator);
            ks.addAll(map.keySet());
            for(K k : ks) {
                entryDeparser.deparseSimple(b, k, map.get(k));
            }
        }

        @Override
        protected void deparseConflict(ConflictDeparseBuilder b, String path, C context, Map<K, V> lhs, Map<K, V> mhs, Map<K, V> rhs) {
            Set<K> ks = Sets.newTreeSet(comparator);
            ks.addAll(lhs.keySet());
            ks.addAll(mhs.keySet());
            ks.addAll(rhs.keySet());
            for(K k : ks) {
                entryDeparser.deparse(b, addPath(path, keyPath(k)), k, lhs.get(k), mhs.get(k), rhs.get(k));
            }
        }

        protected String keyPath(K k) {
            return k.toString();
        }
    }

    private static final Deparser<String, String> packageMetadataItemDeparser = new ConflictMarkerDeparser<String, String>() {
        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, String k, String v) {
            b.add("        Metadata:" + k + "=" + v);
        }
    };

    private static final Deparser<ObjectUtils.Null, Map<String, String>> packageMetadataDeparser = new MapDeparser<ObjectUtils.Null, String, String>(Ordering.<String>natural(), packageMetadataItemDeparser);

    private static final Deparser<Pair<NormalDependencyType, String>, String> normalDepDeparser = new ConflictMarkerDeparser<Pair<NormalDependencyType, String>, String>() {
        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, Pair<NormalDependencyType, String> k, String v) {
            b.add("        " + k.getLeft().getTag() + ":" + k.getRight() + "," + v);
        }
    };

    private static final Comparator<Pair<NormalDependencyType, String>> normalDepComparator = new Comparator<Pair<NormalDependencyType, String>>() {
        @Override
        public int compare(Pair<NormalDependencyType, String> a, Pair<NormalDependencyType, String> b) {
            int r1 = a.getLeft().compareTo(b.getLeft());
            if(r1 != 0) {
                return r1;
            }
            return a.getRight().compareTo(b.getRight());
        }
    };

    private static final Deparser<ObjectUtils.Null, Map<Pair<NormalDependencyType, String>, String>> packageNormalDepsDeparser = new MapDeparser<ObjectUtils.Null, Pair<NormalDependencyType, String>, String>(normalDepComparator, normalDepDeparser) {
        @Override
        protected String keyPath(Pair<NormalDependencyType, String> p) {
            return p.getLeft().getTag() + "/" + p.getRight();
        }
    };

    private static final Deparser<PackageTip, String> replaceDepDeparser = new ConflictMarkerDeparser<PackageTip, String>() {
        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, PackageTip k, String v) {
            b.add("        Replace:" + k.name + "," + k.tip + "," + v);
        }
    };

    private static final Deparser<ObjectUtils.Null, Map<PackageTip, String>> replaceDepsDeparser = new MapDeparser<ObjectUtils.Null, PackageTip, String>(PackageTip.TYPE.COMPARATOR, replaceDepDeparser);

    private static final Deparser<Pair<PackageTip, String>, ObjectUtils.Null> verifyDepDeparser = new ConflictMarkerDeparser<Pair<PackageTip, String>, ObjectUtils.Null>() {
        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, Pair<PackageTip, String> k, ObjectUtils.Null v) {
            b.add("        Verify:" + k.getLeft().name + "," + k.getLeft().tip + "," + k.getRight());
        }
    };

    public static final Comparator<Pair<PackageTip, String>> verifyDepComparator = new Comparator<Pair<PackageTip, String>>() {
        @Override
        public int compare(Pair<PackageTip, String> a, Pair<PackageTip, String> b) {
            int r1 = PackageTip.TYPE.COMPARATOR.compare(a.getLeft(), b.getLeft());
            if(r1 != 0) {
                return r1;
            }
            return a.getRight().compareTo(b.getRight());
        }
    };

    private static final Deparser<ObjectUtils.Null, Map<Pair<PackageTip, String>, ObjectUtils.Null>> verifyDepsDeparser = new MapDeparser<ObjectUtils.Null, Pair<PackageTip, String>, ObjectUtils.Null>(verifyDepComparator, verifyDepDeparser) {
        @Override
        protected String keyPath(Pair<PackageTip, String> p) {
            return p.getLeft() + "/" + p.getRight();
        }
    };

    private static final Deparser<String, PackageManifest> packageManifestDeparser = new Deparser<String, PackageManifest>() {
        private Map<Pair<NormalDependencyType, String>, String> invertNormalDeps(Map<String, Pair<NormalDependencyType, String>> normalDeps) {
            ImmutableMap.Builder<Pair<NormalDependencyType, String>, String> b = ImmutableMap.builder();
            Map<NormalDependencyType, Map<String, String>> ret = Maps.newHashMap();
            for(Map.Entry<String, Pair<NormalDependencyType, String>> e : normalDeps.entrySet()) {
                NormalDependencyType normalDependencyType = e.getValue().getLeft();
                b.put(Pair.of(e.getValue().getLeft(), e.getKey()), e.getValue().getRight());
            }
            return b.build();
        }

        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, String k, PackageManifest v) {
            b.add("    " + k);
            packageMetadataDeparser.deparseSimple(b, ObjectUtils.NULL, v.metadata.toStringMap());
            packageNormalDepsDeparser.deparseSimple(b, ObjectUtils.NULL, invertNormalDeps(v.normalDeps));
            replaceDepsDeparser.deparseSimple(b, ObjectUtils.NULL, v.replaceDeps);
            verifyDepsDeparser.deparseSimple(b, ObjectUtils.NULL, v.verifyDeps);
        }

        @Override
        protected void deparseConflict(ConflictDeparseBuilder b, String path, String k, PackageManifest lhs, PackageManifest mhs, PackageManifest rhs) {
            b.add("    " + k);
            packageMetadataDeparser.deparse(b, addPath(path, "metadata"), ObjectUtils.NULL, lhs.metadata.toStringMap(), mhs.metadata.toStringMap(), rhs.metadata.toStringMap());
            packageNormalDepsDeparser.deparse(b, addPath(path, "normalDeps"), ObjectUtils.NULL, invertNormalDeps(lhs.normalDeps), invertNormalDeps(mhs.normalDeps), invertNormalDeps(rhs.normalDeps));
            replaceDepsDeparser.deparse(b, addPath(path, "replaceDeps"), ObjectUtils.NULL, lhs.replaceDeps, mhs.replaceDeps, rhs.replaceDeps);
            verifyDepsDeparser.deparse(b, addPath(path, "verifyDeps"), ObjectUtils.NULL, lhs.verifyDeps, mhs.verifyDeps, rhs.verifyDeps);
        }
    };

    private static final Deparser<RepoTip, VcsVersionDigest> repoVersionDeparser = new ConflictMarkerDeparser<RepoTip, VcsVersionDigest>() {
        @Override
        public void deparseSimple(SimpleDeparseBuilder b, RepoTip repo, VcsVersionDigest v) {
            b.add(repo.name + "," + repo.tip + ":" + v.getRawDigest());
        }
    };

    private static final Deparser<ObjectUtils.Null, Map<String, PackageManifest>> repoManifestMapDeparser = new MapDeparser<ObjectUtils.Null, String, PackageManifest>(Ordering.<String>natural(), packageManifestDeparser);

    private static final Deparser<RepoTip, RepoManifest> repoManifestDeparser = new Deparser<RepoTip, RepoManifest>() {
        @Override
        public void deparseSimple(SimpleDeparseBuilder b, RepoTip k, RepoManifest v) {
            repoVersionDeparser.deparseSimple(b, k, v.version);
            repoManifestMapDeparser.deparseSimple(b, ObjectUtils.NULL, v.packages);
        }

        @Override
        public void deparseConflict(ConflictDeparseBuilder b, String path, RepoTip k, RepoManifest lhs, RepoManifest mhs, RepoManifest rhs) {
            repoVersionDeparser.deparse(b, addPath(path, "version"), k, lhs.version, mhs.version, rhs.version);
            repoManifestMapDeparser.deparse(b, addPath(path, "packages"), ObjectUtils.NULL, lhs.packages, mhs.packages, rhs.packages);
        }
    };

    private static final Deparser<ObjectUtils.Null, QbtManifest> qbtManifestDeparser = new Deparser<ObjectUtils.Null, QbtManifest>() {
        @Override
        public void deparseSimple(SimpleDeparseBuilder b, ObjectUtils.Null context, QbtManifest v) {
            qbtManifestMapDeparser.deparseSimple(b, context, v.repos);
        }

        @Override
        protected void deparseConflict(ConflictDeparseBuilder b, String path, ObjectUtils.Null context, QbtManifest lhs, QbtManifest mhs, QbtManifest rhs) {
            qbtManifestMapDeparser.deparse(b, path, context, lhs.repos, mhs.repos, rhs.repos);
        }
    };

    private static final Deparser<ObjectUtils.Null, Map<RepoTip, RepoManifest>> qbtManifestMapDeparser = new MapDeparser<ObjectUtils.Null, RepoTip, RepoManifest>(RepoTip.TYPE.COMPARATOR, repoManifestDeparser);

    public static Pair<ImmutableList<Pair<String, String>>, ImmutableList<String>> deparseConflicts(final String lhsName, QbtManifest lhs, final String mhsName, QbtManifest mhs, final String rhsName, QbtManifest rhs) {
        final ImmutableList.Builder<Pair<String, String>> conflicts = ImmutableList.builder();
        final ImmutableList.Builder<String> lines = ImmutableList.builder();
        ConflictDeparseBuilder b = new ConflictDeparseBuilder() {
            @Override
            public void add(String line) {
                lines.add(line);
            }

            @Override
            public void addConflict(String path, String type, Iterable<String> lhs, Iterable<String> mhs, Iterable<String> rhs) {
                conflicts.add(Pair.of(path, type));
                lines.add("<<<<<<< " + lhsName);
                lines.addAll(lhs);
                lines.add("||||||| " + mhsName);
                lines.addAll(mhs);
                lines.add("=======");
                lines.addAll(rhs);
                lines.add(">>>>>>> " + rhsName);
            }
        };
        qbtManifestDeparser.deparse(b, "", ObjectUtils.NULL, lhs, mhs, rhs);
        return Pair.of(conflicts.build(), lines.build());
    }

    public ImmutableList<String> deparse() {
        PoolSimpleDeparseBuilder b = new PoolSimpleDeparseBuilder();
        qbtManifestDeparser.deparseSimple(b, ObjectUtils.NULL, this);
        return b.build();
    }

    public static final MapStructType<QbtManifest, Builder, RepoTip, RepoManifest, RepoManifest.Builder> TYPE = new MapStructType<QbtManifest, Builder, RepoTip, RepoManifest, RepoManifest.Builder>() {
        @Override
        protected QbtManifest create(ImmutableMap<RepoTip, RepoManifest> map) {
            return new QbtManifest(map);
        }

        @Override
        protected Builder createBuilder(ImmutableSalvagingMap<RepoTip, RepoManifest.Builder> map) {
            return new Builder(map);
        }

        @Override
        protected RepoManifest toStruct(RepoManifest.Builder vb) {
            return vb.build();
        }

        @Override
        protected RepoManifest.Builder toBuilder(RepoManifest vs) {
            return vs.builder();
        }

        @Override
        protected Merge<RepoManifest> mergeValue() {
            return RepoManifest.TYPE.merge();
        }
    };
}
