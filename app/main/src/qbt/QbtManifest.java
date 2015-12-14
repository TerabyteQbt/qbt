package qbt;

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
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public final class QbtManifest {
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
        this.repos = repos;

        ImmutableMap.Builder<PackageTip, RepoTip> packageToRepoBuilder = ImmutableMap.builder();
        for(Map.Entry<RepoTip, RepoManifest> e : repos.entrySet()) {
            for(String pkg : e.getValue().packages.keySet()) {
                packageToRepoBuilder.put(PackageTip.TYPE.of(pkg, e.getKey().tip), e.getKey());
            }
        }
        this.packageToRepo = packageToRepoBuilder.build();
    }

    private QbtManifest(Builder b) {
        this(ImmutableMap.copyOf(b.repos));
    }

    public static class Builder {
        private final Map<RepoTip, RepoManifest> repos = Maps.newHashMap();

        private Builder() {
        }

        private Builder(QbtManifest manifest) {
            repos.putAll(manifest.repos);
        }

        public Builder with(RepoTip repo, RepoManifest manifest) {
            repos.put(repo, manifest);
            return this;
        }

        public QbtManifest build() {
            return new QbtManifest(this);
        }
    }

    public static QbtManifest parse(Path f) throws IOException {
        return parse(f.toAbsolutePath().toString(), QbtUtils.readLines(f));
    }

    private static final class Parser {
        private Builder b = new Builder();
        private RepoTip currentRepo = null;
        private RepoManifest.Builder repoBuilder = null;
        private String currentPackage = null;
        private PackageManifest.Builder packageBuilder = null;

        private void closePackage() {
            if(packageBuilder == null) {
                return;
            }

            PackageManifest completePackage = packageBuilder.build();
            repoBuilder = repoBuilder.with(currentPackage, completePackage);
            currentPackage = null;
            packageBuilder = null;
        }

        private void closeRepo() {
            if(repoBuilder == null) {
                return;
            }
            closePackage();
            RepoManifest completeRepo = repoBuilder.build();
            b = b.with(currentRepo, completeRepo);
            currentRepo = null;
            repoBuilder = null;
        }

        public void line(String line) {
            Matcher repoMatcher = REPO_PATTERN.matcher(line);
            if(repoMatcher.matches()) {
                closeRepo();
                currentRepo = RepoTip.TYPE.of(repoMatcher.group(1), repoMatcher.group(2));
                repoBuilder = RepoManifest.builder(new VcsVersionDigest(QbtHashUtils.parse(repoMatcher.group(3))));
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
                packageBuilder = PackageManifest.emptyBuilder().withStringMetadata("prefix", oldPackageMatcher.group(2));
                return;
            }
            else if(packageMatcher.matches()) {
                closePackage();
                currentPackage = packageMatcher.group(1);
                packageBuilder = PackageManifest.emptyBuilder();
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

        public Iterable<String> build() {
            return b.build();
        }
    }

    private static interface ConflictDeparseBuilder extends SimpleDeparseBuilder {
        public void addConflict(Iterable<String> lhs, Iterable<String> mhs, Iterable<String> rhs);
    }

    private static abstract class Deparser<K, V> {
        public final void deparse(ConflictDeparseBuilder b, K k, V lhs, V mhs, V rhs) {
            if(Objects.equal(mhs, lhs)) {
                deparseSimple(b, k, rhs);
                return;
            }
            if(Objects.equal(mhs, rhs)) {
                deparseSimple(b, k, lhs);
                return;
            }
            if(Objects.equal(lhs, rhs)) {
                deparseSimple(b, k, lhs);
                return;
            }
            if(lhs == null || mhs == null || rhs == null) {
                PoolSimpleDeparseBuilder lhsLines = new PoolSimpleDeparseBuilder();
                if(lhs != null) {
                    deparseSimple(lhsLines, k, lhs);
                }
                PoolSimpleDeparseBuilder mhsLines = new PoolSimpleDeparseBuilder();
                if(mhs != null) {
                    deparseSimple(mhsLines, k, mhs);
                }
                PoolSimpleDeparseBuilder rhsLines = new PoolSimpleDeparseBuilder();
                if(rhs != null) {
                    deparseSimple(rhsLines, k, rhs);
                }
                b.addConflict(lhsLines.build(), mhsLines.build(), rhsLines.build());
                return;
            }
            deparseConflict(b, k, lhs, mhs, rhs);
        }

        protected abstract void deparseSimple(SimpleDeparseBuilder b, K k, V v);
        protected abstract void deparseConflict(ConflictDeparseBuilder b, K k, V lhs, V mhs, V rhs);
    }

    private static abstract class ConflictMarkerDeparser<K, V> extends Deparser<K, V> {
        @Override
        protected void deparseConflict(ConflictDeparseBuilder b, K k, V lhs, V mhs, V rhs) {
            PoolSimpleDeparseBuilder lhsLines = new PoolSimpleDeparseBuilder();
            deparseSimple(lhsLines, k, lhs);
            PoolSimpleDeparseBuilder mhsLines = new PoolSimpleDeparseBuilder();
            deparseSimple(mhsLines, k, mhs);
            PoolSimpleDeparseBuilder rhsLines = new PoolSimpleDeparseBuilder();
            deparseSimple(rhsLines, k, rhs);
            b.addConflict(lhsLines.build(), mhsLines.build(), rhsLines.build());
        }
    }

    private static class MapDeparser<K1, K2, V> extends Deparser<K1, Map<K2, V>> {
        private final Comparator<K2> comparator;
        private final Deparser<Pair<K1, K2>, V> entryDeparser;

        public MapDeparser(Comparator<K2> comparator, Deparser<Pair<K1, K2>, V> entryDeparser) {
            this.comparator = comparator;
            this.entryDeparser = entryDeparser;
        }

        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, K1 k1, Map<K2, V> map) {
            Set<K2> k2s = Sets.newTreeSet(comparator);
            k2s.addAll(map.keySet());
            for(K2 k2 : k2s) {
                entryDeparser.deparseSimple(b, Pair.of(k1, k2), map.get(k2));
            }
        }

        @Override
        protected void deparseConflict(ConflictDeparseBuilder b, K1 k1, Map<K2, V> lhs, Map<K2, V> mhs, Map<K2, V> rhs) {
            Set<K2> k2s = Sets.newTreeSet(comparator);
            k2s.addAll(lhs.keySet());
            k2s.addAll(mhs.keySet());
            k2s.addAll(rhs.keySet());
            for(K2 k2 : k2s) {
                entryDeparser.deparse(b, Pair.of(k1, k2), lhs.get(k2), mhs.get(k2), rhs.get(k2));
            }
        }
    }

    private static class SetDeparser<K1, K2> extends Deparser<K1, Set<K2>> {
        private final Comparator<K2> comparator;
        private final Deparser<Pair<K1, K2>, ObjectUtils.Null> entryDeparser;

        public SetDeparser(Comparator<K2> comparator, Deparser<Pair<K1, K2>, ObjectUtils.Null> entryDeparser) {
            this.comparator = comparator;
            this.entryDeparser = entryDeparser;
        }

        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, K1 k1, Set<K2> set) {
            Set<K2> k2s = Sets.newTreeSet(comparator);
            k2s.addAll(set);
            for(K2 k2 : k2s) {
                entryDeparser.deparseSimple(b, Pair.of(k1, k2), ObjectUtils.NULL);
            }
        }

        @Override
        protected void deparseConflict(ConflictDeparseBuilder b, K1 k1, Set<K2> lhs, Set<K2> mhs, Set<K2> rhs) {
            Set<K2> k2s = Sets.newTreeSet(comparator);
            k2s.addAll(lhs);
            k2s.addAll(mhs);
            k2s.addAll(rhs);
            for(K2 k2 : k2s) {
                boolean l = lhs.contains(k2);
                boolean m = mhs.contains(k2);
                boolean r = rhs.contains(k2);
                boolean keep;
                if(m) {
                    // was present, either (or both) could have removed
                    keep = l && r;
                }
                else {
                    // wasn't present, either (or both) could add
                    keep = l || r;
                }
                if(keep) {
                    entryDeparser.deparseSimple(b, Pair.of(k1, k2), ObjectUtils.NULL);
                }
            }
        }
    }

    private static final Deparser<Pair<ObjectUtils.Null, String>, String> packageMetadataItemDeparser = new ConflictMarkerDeparser<Pair<ObjectUtils.Null, String>, String>() {
        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, Pair<ObjectUtils.Null, String> k, String v) {
            b.add("        Metadata:" + k.getRight() + "=" + v);
        }
    };

    private static final Deparser<ObjectUtils.Null, Map<String, String>> packageMetadataDeparser = new MapDeparser<ObjectUtils.Null, String, String>(Ordering.<String>natural(), packageMetadataItemDeparser);

    private static final Deparser<Pair<Pair<ObjectUtils.Null, NormalDependencyType>, String>, String> normalDepDeparser = new ConflictMarkerDeparser<Pair<Pair<ObjectUtils.Null, NormalDependencyType>, String>, String>() {
        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, Pair<Pair<ObjectUtils.Null, NormalDependencyType>, String> k, String v) {
            b.add("        " + k.getLeft().getRight().getTag() + ":" + k.getRight() + "," + v);
        }
    };

    private static final Deparser<Pair<ObjectUtils.Null, NormalDependencyType>, Map<String, String>> normalDepTypeDeparser = new MapDeparser<Pair<ObjectUtils.Null, NormalDependencyType>, String, String>(Ordering.<String>natural(), normalDepDeparser);

    private static final Deparser<ObjectUtils.Null, Map<NormalDependencyType, Map<String, String>>> packageNormalDepsDeparser = new MapDeparser<ObjectUtils.Null, NormalDependencyType, Map<String, String>>(Ordering.<NormalDependencyType>natural(), normalDepTypeDeparser);

    private static final Deparser<Pair<ObjectUtils.Null, PackageTip>, String> replaceDepDeparser = new ConflictMarkerDeparser<Pair<ObjectUtils.Null, PackageTip>, String>() {
        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, Pair<ObjectUtils.Null, PackageTip> k, String v) {
            b.add("        Replace:" + k.getRight().name + "," + k.getRight().tip + "," + v);
        }
    };

    private static final Deparser<ObjectUtils.Null, Map<PackageTip, String>> replaceDepsDeparser = new MapDeparser<ObjectUtils.Null, PackageTip, String>(PackageTip.TYPE.COMPARATOR, replaceDepDeparser);

    private static final Deparser<Pair<ObjectUtils.Null, Pair<PackageTip, String>>, ObjectUtils.Null> verifyDepDeparser = new ConflictMarkerDeparser<Pair<ObjectUtils.Null, Pair<PackageTip, String>>, ObjectUtils.Null>() {
        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, Pair<ObjectUtils.Null, Pair<PackageTip, String>> k, ObjectUtils.Null v) {
            b.add("        Verify:" + k.getRight().getLeft().name + "," + k.getRight().getLeft().tip + "," + k.getRight().getRight());
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

    private static final Deparser<ObjectUtils.Null, Set<Pair<PackageTip, String>>> verifyDepsDeparser = new SetDeparser<ObjectUtils.Null, Pair<PackageTip, String>>(verifyDepComparator, verifyDepDeparser);

    private static final Deparser<Pair<RepoTip, String>, PackageManifest> packageManifestDeparser = new Deparser<Pair<RepoTip, String>, PackageManifest>() {
        private Map<NormalDependencyType, Map<String, String>> invertNormalDeps(Map<String, Pair<NormalDependencyType, String>> normalDeps) {
            Map<NormalDependencyType, Map<String, String>> ret = Maps.newHashMap();
            for(Map.Entry<String, Pair<NormalDependencyType, String>> e : normalDeps.entrySet()) {
                NormalDependencyType normalDependencyType = e.getValue().getLeft();
                Map<String, String> subRet = ret.get(normalDependencyType);
                if(subRet == null) {
                    ret.put(normalDependencyType, subRet = Maps.newHashMap());
                }
                subRet.put(e.getKey(), e.getValue().getRight());
            }
            return ret;
        }

        @Override
        protected void deparseSimple(SimpleDeparseBuilder b, Pair<RepoTip, String> k, PackageManifest v) {
            b.add("    " + k.getRight());
            packageMetadataDeparser.deparseSimple(b, ObjectUtils.NULL, v.metadata.toStringMap());
            packageNormalDepsDeparser.deparseSimple(b, ObjectUtils.NULL, invertNormalDeps(v.normalDeps));
            replaceDepsDeparser.deparseSimple(b, ObjectUtils.NULL, v.replaceDeps);
            verifyDepsDeparser.deparseSimple(b, ObjectUtils.NULL, v.verifyDeps);
        }

        @Override
        protected void deparseConflict(ConflictDeparseBuilder b, Pair<RepoTip, String> k, PackageManifest lhs, PackageManifest mhs, PackageManifest rhs) {
            b.add("    " + k.getRight());
            packageMetadataDeparser.deparseConflict(b, ObjectUtils.NULL, lhs.metadata.toStringMap(), mhs.metadata.toStringMap(), rhs.metadata.toStringMap());
            packageNormalDepsDeparser.deparseConflict(b, ObjectUtils.NULL, invertNormalDeps(lhs.normalDeps), invertNormalDeps(mhs.normalDeps), invertNormalDeps(rhs.normalDeps));
            replaceDepsDeparser.deparseConflict(b, ObjectUtils.NULL, lhs.replaceDeps, mhs.replaceDeps, rhs.replaceDeps);
            verifyDepsDeparser.deparseConflict(b, ObjectUtils.NULL, lhs.verifyDeps, mhs.verifyDeps, rhs.verifyDeps);
        }
    };

    private static final Deparser<RepoTip, VcsVersionDigest> repoVersionDeparser = new ConflictMarkerDeparser<RepoTip, VcsVersionDigest>() {
        @Override
        public void deparseSimple(SimpleDeparseBuilder b, RepoTip repo, VcsVersionDigest v) {
            b.add(repo.name + "," + repo.tip + ":" + v.getRawDigest());
        }
    };

    private static final Deparser<RepoTip, Map<String, PackageManifest>> repoManifestMapDeparser = new MapDeparser<RepoTip, String, PackageManifest>(Ordering.<String>natural(), packageManifestDeparser);

    private static final Deparser<Pair<ObjectUtils.Null, RepoTip>, RepoManifest> repoManifestDeparser = new Deparser<Pair<ObjectUtils.Null, RepoTip>, RepoManifest>() {
        @Override
        public void deparseSimple(SimpleDeparseBuilder b, Pair<ObjectUtils.Null, RepoTip> k, RepoManifest v) {
            repoVersionDeparser.deparseSimple(b, k.getRight(), v.version);
            repoManifestMapDeparser.deparseSimple(b, k.getRight(), v.packages);
        }

        @Override
        public void deparseConflict(ConflictDeparseBuilder b, Pair<ObjectUtils.Null, RepoTip> k, RepoManifest lhs, RepoManifest mhs, RepoManifest rhs) {
            repoVersionDeparser.deparse(b, k.getRight(), lhs.version, mhs.version, rhs.version);
            repoManifestMapDeparser.deparse(b, k.getRight(), lhs.packages, mhs.packages, rhs.packages);
        }
    };

    private static final Deparser<ObjectUtils.Null, QbtManifest> qbtManifestDeparser = new Deparser<ObjectUtils.Null, QbtManifest>() {
        @Override
        public void deparseSimple(SimpleDeparseBuilder b, ObjectUtils.Null k, QbtManifest v) {
            qbtManifestMapDeparser.deparseSimple(b, k, v.repos);
        }

        @Override
        protected void deparseConflict(ConflictDeparseBuilder b, ObjectUtils.Null k, QbtManifest lhs, QbtManifest mhs, QbtManifest rhs) {
            qbtManifestMapDeparser.deparse(b, k, lhs.repos, mhs.repos, rhs.repos);
        }
    };

    private static final Deparser<ObjectUtils.Null, Map<RepoTip, RepoManifest>> qbtManifestMapDeparser = new MapDeparser<ObjectUtils.Null, RepoTip, RepoManifest>(RepoTip.TYPE.COMPARATOR, repoManifestDeparser);

    public static Pair<Boolean, Iterable<String>> deparseConflicts(final String lhsName, QbtManifest lhs, final String mhsName, QbtManifest mhs, final String rhsName, QbtManifest rhs) {
        final ImmutableList.Builder<String> ret = ImmutableList.builder();
        final MutableBoolean conflicted = new MutableBoolean(false);
        ConflictDeparseBuilder b = new ConflictDeparseBuilder() {
            @Override
            public void add(String line) {
                ret.add(line);
            }

            @Override
            public void addConflict(Iterable<String> lhs, Iterable<String> mhs, Iterable<String> rhs) {
                ret.add("<<<<<<< " + lhsName);
                ret.addAll(lhs);
                ret.add("||||||| " + mhsName);
                ret.addAll(mhs);
                ret.add("=======");
                ret.addAll(rhs);
                ret.add(">>>>>>> " + rhsName);
                conflicted.setTrue();
            }
        };
        qbtManifestDeparser.deparse(b, ObjectUtils.NULL, lhs, mhs, rhs);
        return Pair.<Boolean, Iterable<String>>of(conflicted.booleanValue(), ret.build());
    }

    public Iterable<String> deparse() {
        PoolSimpleDeparseBuilder b = new PoolSimpleDeparseBuilder();
        qbtManifestDeparser.deparseSimple(b, ObjectUtils.NULL, this);
        return b.build();
    }

    public Builder builder() {
        return new Builder(this);
    }

    public static Builder emptyBuilder() {
        return new Builder();
    }

    @Override
    public int hashCode() {
        return repos.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof QbtManifest)) {
            return false;
        }
        QbtManifest other = (QbtManifest)obj;
        if(!repos.equals(other.repos)) {
            return false;
        }
        return true;
    }

    public static QbtManifest of(Map<RepoTip, RepoManifest> repos) {
        return new QbtManifest(ImmutableMap.copyOf(repos));
    }
}
