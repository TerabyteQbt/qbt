package qbt.map;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import misc1.commons.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.config.QbtConfig;
import qbt.manifest.current.PackageManifest;
import qbt.manifest.current.PackageMetadata;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.recursive.cv.CumulativeVersionDigest;
import qbt.recursive.cv.CumulativeVersionNodeData;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.recursive.cvrpd.CvRecursivePackageDataVersionAdder;
import qbt.recursive.srpd.SimpleRecursivePackageData;
import qbt.recursive.srpd.SimpleRecursivePackageDataCanonicalizer;
import qbt.recursive.srpd.SimpleRecursivePackageDataTransformer;
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

    private final QbtConfig config;
    private final DependencyComputer dependencyComputer;

    public CumulativeVersionComputer(QbtConfig config, QbtManifest manifest) {
        this.config = config;
        this.dependencyComputer = new DependencyComputer(manifest);
    }

    private final LoadingCache<Pair<RepoTip, VcsVersionDigest>, Pair<CommonRepoAccessor, VcsTreeDigest>> repoCache = CacheBuilder.newBuilder().build(new CacheLoader<Pair<RepoTip, VcsVersionDigest>, Pair<CommonRepoAccessor, VcsTreeDigest>>() {
        @Override
        public Pair<CommonRepoAccessor, VcsTreeDigest> load(Pair<RepoTip, VcsVersionDigest> input) {
            RepoTip repo = input.getLeft();
            VcsVersionDigest version = input.getRight();

            CommonRepoAccessor commonRepoAccessor = config.requireCommonRepo(repo, version);
            VcsTreeDigest repoTree = commonRepoAccessor.getEffectiveTree(Maybe.of(""));

            return Pair.of(commonRepoAccessor, repoTree);
        }
    });

    private final SimpleRecursivePackageDataTransformer<DependencyComputer.Result, Result> requireRepoTransformer = new SimpleRecursivePackageDataTransformer<DependencyComputer.Result, Result>() {
        @Override
        protected Result transformResult(DependencyComputer.Result result, Map<String, Pair<NormalDependencyType, SimpleRecursivePackageData<Result>>> dependencyResults) {
            PackageTip packageTip = result.packageTip;
            RepoTip repo = result.repo;
            RepoManifest repoManifest = result.repoManifest;
            PackageManifest packageManifest = result.packageManifest;
            VcsVersionDigest version = repoManifest.version;

            Pair<CommonRepoAccessor, VcsTreeDigest> repoCacheResult = repoCache.getUnchecked(Pair.of(repo, version));
            CommonRepoAccessor commonRepoAccessor = repoCacheResult.getLeft();
            VcsTreeDigest repoTree = repoCacheResult.getRight();

            Maybe<String> prefix = packageManifest.metadata.get(PackageMetadata.PREFIX);
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
        return compute(new DependencyComputer.CacheKey(packageTip));
    }

    public CvRecursivePackageData<Result> compute(DependencyComputer.CacheKey key) {
        SimpleRecursivePackageData<DependencyComputer.Result> dcResult = dependencyComputer.compute(key);
        SimpleRecursivePackageData<Result> raw = requireRepoTransformer.transform(dcResult);
        SimpleRecursivePackageData<Result> canonicalized = canonicalizer.transform(raw);
        CvRecursivePackageData<Result> versioned = versionAdder.transform(canonicalized);
        return versioned;
    }

    protected abstract K canonicalizationKey(Result result);

    protected Map<String, String> getQbtEnv() {
        return ImmutableMap.of();
    }
}
