package qbt.map;

import java.util.Map;
import misc1.commons.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import qbt.NormalDependencyType;
import qbt.PackageManifest;
import qbt.PackageTip;
import qbt.RepoManifest;
import qbt.VcsVersionDigest;
import qbt.config.RepoConfig;
import qbt.metadata.PackageMetadataType;
import qbt.recursive.cv.CumulativeVersionDigest;
import qbt.recursive.cv.CumulativeVersionNodeData;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.recursive.cvrpd.CvRecursivePackageDataVersionAdder;
import qbt.recursive.srpd.SimpleRecursivePackageData;
import qbt.recursive.srpd.SimpleRecursivePackageDataCanonicalizer;

public abstract class CumulativeVersionComputer<K> {
    public static final class Result {
        public final PackageTip packageTip;
        public final RepoConfig.RequireRepoResult requireRepoResult;
        public final PackageManifest packageManifest;
        public final CumulativeVersionNodeData cumulativeVersionNodeData;

        public Result(PackageTip packageTip, RepoConfig.RequireRepoResult requireRepoResult, PackageManifest packageManifest, CumulativeVersionNodeData cumulativeVersionNodeData) {
            this.packageTip = packageTip;
            this.requireRepoResult = requireRepoResult;
            this.packageManifest = packageManifest;
            this.cumulativeVersionNodeData = cumulativeVersionNodeData;
        }
    }

    private final DependencyComputer<?, SimpleRecursivePackageData<Result>> dependencyComputer = new DependencyComputer<Result, SimpleRecursivePackageData<Result>>() {
        @Override
        protected Result premap(PackageTip packageTip) {
            Triple<PackageTip, RepoManifest, PackageManifest> requireManifestTriple = requireManifest(packageTip);
            PackageTip repo = requireManifestTriple.getLeft();
            RepoManifest repoManifest = requireManifestTriple.getMiddle();
            PackageManifest packageManifest = requireManifestTriple.getRight();
            VcsVersionDigest version = repoManifest.version;
            RepoConfig.RequireRepoResult requireRepoResult = requireRepo(repo, version);
            Maybe<String> prefix = packageManifest.metadata.get(PackageMetadataType.PREFIX);
            CumulativeVersionNodeData cumulativeVersionNodeData = new CumulativeVersionNodeData(packageTip.pkg, requireRepoResult.getEffectiveTree(prefix), CumulativeVersionDigest.QBT_VERSION, packageManifest.metadata, getQbtEnv());
            return new Result(packageTip, requireRepoResult, packageManifest, cumulativeVersionNodeData);
        }

        @Override
        protected Map<String, Pair<NormalDependencyType, String>> getNormalDeps(Result intermediate, PackageTip packageTip) {
            return intermediate.packageManifest.normalDeps;
        }

        @Override
        protected Map<PackageTip, String> getReplaceDeps(Result intermediate, PackageTip packageTip) {
            return intermediate.packageManifest.replaceDeps;
        }

        @Override
        protected SimpleRecursivePackageData<Result> map(Result intermediate, PackageTip packageTip, Map<String, Pair<NormalDependencyType, SimpleRecursivePackageData<Result>>> dependencyResults) {
            return new SimpleRecursivePackageData<Result>(intermediate, dependencyResults);
        }
    };

    // This is unfortunately very complicated.  All users have to decide to
    // what extent they want results to be canonicalized.
    //
    // Something that views the IntermediateResult solely as a way to get the
    // tree (like a build) will only want to canonicalize on CumulativeVersion
    // since packageTip and actual location of package directory isn't
    // important.
    //
    // Something like eclipse gen however will need to view separate tips as
    // separate since the process needs to be run once per tip and can't be
    // shared.
    private final SimpleRecursivePackageDataCanonicalizer<Result, K> canonicalizer = new SimpleRecursivePackageDataCanonicalizer<Result, K>() {
        @Override
        protected K key(SimpleRecursivePackageData<Result> r) {
            return canonicalizationKey(r.result);
        }
    };
    private final CvRecursivePackageDataVersionAdder<Result, SimpleRecursivePackageData<Result>> versionAdder = new CvRecursivePackageDataVersionAdder<Result, SimpleRecursivePackageData<Result>>() {
        @Override
        protected CumulativeVersionNodeData nodeData(Result result) {
            return result.cumulativeVersionNodeData;
        }
    };
    public CvRecursivePackageData<Result> compute(PackageTip packageTip) {
        SimpleRecursivePackageData<Result> raw = dependencyComputer.compute(packageTip);
        SimpleRecursivePackageData<Result> canonicalized = canonicalizer.transform(raw);
        CvRecursivePackageData<Result> versioned = versionAdder.transform(canonicalized);
        return versioned;
    }

    protected abstract Triple<PackageTip, RepoManifest, PackageManifest> requireManifest(PackageTip packageTip);
    protected abstract RepoConfig.RequireRepoResult requireRepo(PackageTip repo, VcsVersionDigest version);
    protected abstract K canonicalizationKey(Result result);
    protected abstract Map<String, String> getQbtEnv();
}
