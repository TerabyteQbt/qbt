package qbt.manifest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import misc1.commons.Maybe;
import misc1.commons.merge.Merge;
import misc1.commons.tuple.Misc1PairUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.QbtHashUtils;
import qbt.VcsVersionDigest;
import qbt.manifest.v0.PackageManifest;
import qbt.manifest.v0.PackageMetadata;
import qbt.manifest.v0.QbtManifest;
import qbt.manifest.v0.RepoManifest;
import qbt.manifest.v0.Upgrades;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

class V0QbtManifestVersion extends QbtManifestUpgradeableVersion<QbtManifest, QbtManifest.Builder, qbt.manifest.v1.QbtManifest> {
    public V0QbtManifestVersion(V1QbtManifestVersion nextVersion) {
        super(0, nextVersion, QbtManifest.class, qbt.manifest.v1.QbtManifest.class);
    }

    @Override
    public ImmutableSet<RepoTip> getRepos(QbtManifest manifest) {
        return manifest.map.keySet();
    }

    @Override
    public QbtManifest.Builder builder(QbtManifest manifest) {
        return manifest.builder();
    }

    @Override
    public QbtManifest.Builder withRepoVersion(QbtManifest.Builder builder, RepoTip repo, VcsVersionDigest commit) {
        return builder.with(repo, builder.get(repo).set(RepoManifest.VERSION, commit));
    }

    @Override
    public QbtManifest.Builder withoutRepo(QbtManifest.Builder builder, RepoTip repo) {
        return builder.without(repo);
    }

    @Override
    public QbtManifest build(QbtManifest.Builder builder) {
        return builder.build();
    }

    @Override
    public Merge<QbtManifest> merge() {
        return QbtManifest.TYPE.merge();
    }

    @Override
    public QbtManifestParser<QbtManifest> parser() {
        return new QbtManifestParser<QbtManifest>() {
            @Override
            public QbtManifest parse(List<String> lines) {
                return V0QbtManifestVersion.parse(lines);
            }

            @Override
            public ImmutableList<String> deparse(QbtManifest manifest) {
                return V0QbtManifestVersion.deparse(manifest);
            }

            @Override
            public Pair<ImmutableList<Pair<String, String>>, ImmutableList<String>> deparse(String lhsName, QbtManifest lhs, String mhsName, QbtManifest mhs, String rhsName, QbtManifest rhs) {
                if(lhs.equals(mhs) && mhs.equals(rhs)) {
                    return Pair.of(ImmutableList.<Pair<String, String>>of(), deparse(lhs));
                }
                ImmutableList.Builder<String> b = ImmutableList.builder();
                b.add("<<<<<<< " + lhsName);
                b.addAll(deparse(lhs));
                b.add("||||||| " + mhsName);
                b.addAll(deparse(mhs));
                b.add("=======");
                b.addAll(deparse(rhs));
                b.add(">>>>>>> " + rhsName);
                return Pair.of(ImmutableList.of(Pair.of("manifest", "?")), b.build());
            }
        };
    }

    @Override
    public qbt.manifest.v1.QbtManifest upgrade(QbtManifest manifest) {
        return Upgrades.upgrade_QbtManifest(manifest).build();
    }

    private static final Pattern REPO_PATTERN = Pattern.compile("^([0-9a-zA-Z._]*),([0-9a-zA-Z._]*):([0-9a-f]{40})$");
    private static final Pattern OLD_PACKAGE_PATTERN = Pattern.compile("^    ([0-9a-zA-Z._]*):(.*)$");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^    ([0-9a-zA-Z._]*)$");
    private static final Pattern METADATA_PATTERN = Pattern.compile("^        Metadata:([^=]*)=(.*)$");
    private static final Pattern NORMAL_DEP_PATTERN = Pattern.compile("^        ([A-Za-z]*):([0-9a-zA-Z._]*),([0-9a-zA-Z._]*)$");
    private static final Pattern REPLACE_DEP_PATTERN = Pattern.compile("^        (?:R|Replace):([0-9a-zA-Z._]*),([0-9a-zA-Z._]*),([0-9a-zA-Z._]*)$");
    private static final Pattern VERIFY_DEP_PATTERN = Pattern.compile("^        (?:V|Verify):([0-9a-zA-Z._]*),([0-9a-zA-Z._]*),([0-9a-zA-Z._]*)$");

    private static ImmutableList<String> deparse(QbtManifest manifest) {
        ImmutableList.Builder<String> b = ImmutableList.builder();
        for(RepoTip repo : sort(manifest.map.keySet(), RepoTip.TYPE.COMPARATOR)) {
            RepoManifest repoManifest = manifest.map.get(repo);
            b.add(repo.name + "," + repo.tip + ":" + repoManifest.get(RepoManifest.VERSION).getRawDigest());
            for(String pkg : sort(repoManifest.get(RepoManifest.PACKAGES).map.keySet(), Ordering.<String>natural())) {
                PackageManifest packageManifest = repoManifest.get(RepoManifest.PACKAGES).map.get(pkg);
                b.add("    " + pkg);
                Map<String, String> metadataMap = Maps.newTreeMap();
                for(Map.Entry<String, JsonElement> e : PackageMetadata.SERIALIZER.toJson(packageManifest.get(PackageManifest.METADATA).builder()).getAsJsonObject().entrySet()) {
                    metadataMap.put(e.getKey(), e.getValue().getAsString());
                }
                for(Map.Entry<String, String> e : metadataMap.entrySet()) {
                    b.add("        Metadata:" + e.getKey() + "=" + e.getValue());
                }
                Map<Pair<NormalDependencyType, String>, String> normalDeps = Maps.newTreeMap(Misc1PairUtils.comparator(Ordering.<NormalDependencyType>natural(), Ordering.<String>natural()));
                for(Map.Entry<String, Pair<NormalDependencyType, String>> e : packageManifest.get(PackageManifest.NORMAL_DEPS).map.entrySet()) {
                    normalDeps.put(Pair.of(e.getValue().getLeft(), e.getKey()), e.getValue().getRight());
                }
                for(Map.Entry<Pair<NormalDependencyType, String>, String> e : normalDeps.entrySet()) {
                    b.add("        " + e.getKey().getLeft().getTag() + ":" + e.getKey().getRight() + "," + e.getValue());
                }
                for(PackageTip pkg2 : sort(packageManifest.get(PackageManifest.REPLACE_DEPS).map.keySet(), PackageTip.TYPE.COMPARATOR)) {
                    b.add("        Replace:" + pkg2.name + "," + pkg2.tip + "," + packageManifest.get(PackageManifest.REPLACE_DEPS).map.get(pkg2));
                }
                for(Pair<PackageTip, String> p : sort(packageManifest.get(PackageManifest.VERIFY_DEPS).map.keySet(), Misc1PairUtils.comparator(PackageTip.TYPE.COMPARATOR, Ordering.<String>natural()))) {
                    b.add("        Verify:" + p.getLeft().name + "," + p.getLeft().tip + "," + p.getRight());
                }
            }
        }
        return b.build();
    }

    private static final class Parser {
        private QbtManifest.Builder b = QbtManifest.TYPE.builder();
        private RepoTip currentRepo = null;
        private RepoManifest.Builder repoBuilder = null;
        private String currentPackage = null;
        private PackageManifest.Builder packageBuilder = null;
        private JsonObject packageMetadataBuilder = null;

        private void closePackage() {
            if(packageBuilder == null) {
                return;
            }

            packageBuilder = packageBuilder.set(PackageManifest.METADATA, PackageMetadata.SERIALIZER.fromJson(packageMetadataBuilder));
            repoBuilder = repoBuilder.set(RepoManifest.PACKAGES, repoBuilder.get(RepoManifest.PACKAGES).with(currentPackage, packageBuilder));
            currentPackage = null;
            packageBuilder = null;
            packageMetadataBuilder = null;
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
                packageBuilder = PackageManifest.TYPE.builder().set(PackageManifest.METADATA, PackageMetadata.TYPE.builder().set(PackageMetadata.PREFIX, Maybe.of(oldPackageMatcher.group(2))));
                packageMetadataBuilder = new JsonObject();
                return;
            }
            else if(packageMatcher.matches()) {
                closePackage();
                currentPackage = packageMatcher.group(1);
                packageBuilder = PackageManifest.TYPE.builder();
                packageMetadataBuilder = new JsonObject();
                return;
            }
            else if(currentPackage == null) {
                throw new IllegalArgumentException("expecting package identifier but got '" + line +"'");
            }

            Matcher metadataMatcher = METADATA_PATTERN.matcher(line);
            if(metadataMatcher.matches()) {
                packageMetadataBuilder.addProperty(metadataMatcher.group(1), metadataMatcher.group(2));
                return;
            }

            Matcher normalDepMatcher = NORMAL_DEP_PATTERN.matcher(line);
            if(normalDepMatcher.matches()) {
                packageBuilder = packageBuilder.set(PackageManifest.NORMAL_DEPS, packageBuilder.get(PackageManifest.NORMAL_DEPS).with(normalDepMatcher.group(2), Pair.of(NormalDependencyType.fromTag(normalDepMatcher.group(1)), normalDepMatcher.group(3))));
                return;
            }

            Matcher replaceDepMatcher = REPLACE_DEP_PATTERN.matcher(line);
            if(replaceDepMatcher.matches()) {
                packageBuilder = packageBuilder.set(PackageManifest.REPLACE_DEPS, packageBuilder.get(PackageManifest.REPLACE_DEPS).with(PackageTip.TYPE.of(replaceDepMatcher.group(1), replaceDepMatcher.group(2)), replaceDepMatcher.group(3)));
                return;
            }

            Matcher verifyDepMatcher = VERIFY_DEP_PATTERN.matcher(line);
            if(verifyDepMatcher.matches()) {
                packageBuilder = packageBuilder.set(PackageManifest.VERIFY_DEPS, packageBuilder.get(PackageManifest.VERIFY_DEPS).with(Pair.of(PackageTip.TYPE.of(verifyDepMatcher.group(1), verifyDepMatcher.group(2)), verifyDepMatcher.group(3)), ObjectUtils.NULL));
                return;
            }

            throw new IllegalArgumentException("expecting dependency or metadata line but got '" + line + "'");
        }

        public QbtManifest complete() {
            closeRepo();
            return b.build();
        }
    }

    private static QbtManifest parse(Iterable<String> lines) {
        Parser p = new Parser();
        for(String line : lines) {
            p.line(line);
        }
        return p.complete();
    }

    private static <E> Iterable<E> sort(Collection<E> in, Comparator<E> comparator) {
        Set<E> out = Sets.newTreeSet(comparator);
        out.addAll(in);
        return out;
    }

    private Map<Pair<NormalDependencyType, String>, String> invertNormalDeps(Map<String, Pair<NormalDependencyType, String>> normalDeps) {
        ImmutableMap.Builder<Pair<NormalDependencyType, String>, String> b = ImmutableMap.builder();
        Map<NormalDependencyType, Map<String, String>> ret = Maps.newHashMap();
        for(Map.Entry<String, Pair<NormalDependencyType, String>> e : normalDeps.entrySet()) {
            NormalDependencyType normalDependencyType = e.getValue().getLeft();
            b.put(Pair.of(e.getValue().getLeft(), e.getKey()), e.getValue().getRight());
        }
        return b.build();
    }
}
