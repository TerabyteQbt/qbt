package qbt.map;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Map;
import misc1.commons.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import qbt.NormalDependencyType;
import qbt.PackageManifest;
import qbt.RepoManifest;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.metadata.PackageMetadataType;
import qbt.recursive.cv.CumulativeVersionDigest;
import qbt.recursive.cv.CumulativeVersionNodeData;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.recursive.cvrpd.CvRecursivePackageDataVersionAdder;
import qbt.recursive.srpd.SimpleRecursivePackageData;
import qbt.recursive.srpd.SimpleRecursivePackageDataCanonicalizer;
import qbt.repo.CommonRepoAccessor;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public abstract class CumulativeVersionComputer<K> {
    public static final class Result {
        public final PackageTip packageTip;
        public final CommonRepoAccessor commonRepoAccessor;
        public final PackageManifest packageManifest;
        public final CumulativeVersionNodeData cumulativeVersionNodeData;

        public Result(PackageTip packageTip, CommonRepoAccessor commonRepoAccessor, PackageManifest packageManifest, CumulativeVersionNodeData cumulativeVersionNodeData) {
            this.packageTip = packageTip;
            this.commonRepoAccessor = commonRepoAccessor;
            this.packageManifest = packageManifest;
            this.cumulativeVersionNodeData = cumulativeVersionNodeData;
        }
    }

    private final LoadingCache<Pair<RepoTip, VcsVersionDigest>, Pair<CommonRepoAccessor, VcsTreeDigest>> repoCache = CacheBuilder.newBuilder().build(new CacheLoader<Pair<RepoTip, VcsVersionDigest>, Pair<CommonRepoAccessor, VcsTreeDigest>>() {
        @Override
        public Pair<CommonRepoAccessor, VcsTreeDigest> load(Pair<RepoTip, VcsVersionDigest> input) {
            RepoTip repo = input.getLeft();
            VcsVersionDigest version = input.getRight();

            CommonRepoAccessor commonRepoAccessor = requireRepo(repo, version);
            VcsTreeDigest repoTree = commonRepoAccessor.getEffectiveTree(Maybe.of(""));

            return Pair.of(commonRepoAccessor, repoTree);
        }
    });

    private final DependencyComputer<?, SimpleRecursivePackageData<Result>> dependencyComputer = new DependencyComputer<Result, SimpleRecursivePackageData<Result>>() {
        @Override
        protected Result premap(PackageTip packageTip) {
            Triple<RepoTip, RepoManifest, PackageManifest> requireManifestTriple = requireManifest(packageTip);
            RepoTip repo = requireManifestTriple.getLeft();
            RepoManifest repoManifest = requireManifestTriple.getMiddle();
            PackageManifest packageManifest = requireManifestTriple.getRight();
            VcsVersionDigest version = repoManifest.version;

            Pair<CommonRepoAccessor, VcsTreeDigest> repoCacheResult = repoCache.getUnchecked(Pair.of(repo, version));
            CommonRepoAccessor commonRepoAccessor = repoCacheResult.getLeft();
            VcsTreeDigest repoTree = repoCacheResult.getRight();

            Maybe<String> prefix = packageManifest.metadata.get(PackageMetadataType.PREFIX);
            VcsTreeDigest packageTree;
            if(prefix.isPresent()) {
                packageTree = commonRepoAccessor.getSubtree(repoTree, prefix.get(null));
            }
            else {
                packageTree = commonRepoAccessor.getEffectiveTree(prefix);
            }
            CumulativeVersionNodeData cumulativeVersionNodeData = new CumulativeVersionNodeData(packageTip.name, packageTree, CumulativeVersionDigest.QBT_VERSION, packageManifest.metadata, getQbtEnv());
            return new Result(packageTip, commonRepoAccessor, packageManifest, cumulativeVersionNodeData);
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

    protected abstract Triple<RepoTip, RepoManifest, PackageManifest> requireManifest(PackageTip packageTip);
    protected abstract CommonRepoAccessor requireRepo(RepoTip repo, VcsVersionDigest version);
    protected abstract K canonicalizationKey(Result result);
    protected abstract Map<String, String> getQbtEnv();
}
