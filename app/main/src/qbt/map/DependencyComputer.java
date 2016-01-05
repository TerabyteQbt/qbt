package qbt.map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.manifest.current.PackageManifest;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.recursive.srpd.SimpleRecursivePackageData;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public final class DependencyComputer {
    public static final class CacheKey {
        public final PackageTip packageTip;
        public final Map<PackageTip, String> replacements;

        public CacheKey(PackageTip packageTip) {
            this(packageTip, ImmutableMap.<PackageTip, String>of());
        }

        public CacheKey(PackageTip packageTip, Map<PackageTip, String> replacements) {
            this.packageTip = packageTip;
            this.replacements = ImmutableMap.copyOf(replacements);
        }

        @Override
        public int hashCode() {
            int r = 0;
            r = 31 * r + packageTip.hashCode();
            r = 31 * r + replacements.hashCode();
            return r;
        }

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey) o;
            if(!packageTip.equals(other.packageTip)) {
                return false;
            }
            if(!replacements.equals(other.replacements)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return packageTip + "@" + replacements;
        }
    }

    public static final class Result {
        public final CacheKey key;
        public final RepoTip repo;
        public final RepoManifest repoManifest;
        public final PackageTip packageTip;
        public final PackageManifest packageManifest;
        public final ImmutableMap<PackageTip, String> replacementsNext;

        public Result(CacheKey key, RepoTip repo, RepoManifest repoManifest, PackageTip packageTip, PackageManifest packageManifest, ImmutableMap<PackageTip, String> replacementsNext) {
            this.key = key;
            this.repo = repo;
            this.repoManifest = repoManifest;
            this.packageTip = packageTip;
            this.packageManifest = packageManifest;
            this.replacementsNext = replacementsNext;
        }
    }

    private final Map<CacheKey, SimpleRecursivePackageData<Result>> cache = Maps.newHashMap();
    private final QbtManifest manifest;

    public DependencyComputer(QbtManifest manifest) {
        this.manifest = manifest;
    }

    private static class CycleException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private static String message(PackageTip packageTip, Map<PackageTip, String> replacements, CycleException next) {
            return (next == null ? "Cycle at" : "Via") + " " + packageTip + " with replacements " + replacements;
        }

        public CycleException(PackageTip packageTip, Map<PackageTip, String> replacements, CycleException next) {
            super(message(packageTip, replacements, next), next);
        }
    }

    public SimpleRecursivePackageData<Result> compute(PackageTip packageTip) {
        return compute(packageTip, ImmutableMap.<PackageTip, String>of());
    }

    public SimpleRecursivePackageData<Result> compute(PackageTip packageTip, Map<PackageTip, String> replacements) {
        CacheKey key = new CacheKey(packageTip, replacements);
        return compute(key);
    }

    public SimpleRecursivePackageData<Result> compute(CacheKey key) {
        if(cache.containsKey(key)) {
            SimpleRecursivePackageData<Result> result = cache.get(key);
            if(result == null) {
                throw new CycleException(key.packageTip, key.replacements, null);
            }
            return result;
        }
        cache.put(key, null);

        SimpleRecursivePackageData<Result> result = computeUncached(key);
        cache.put(key, result);
        return result;
    }

    private SimpleRecursivePackageData<Result> computeUncached(CacheKey key) {
        PackageTip packageTip = key.packageTip;
        Map<PackageTip, String> replacements = key.replacements;

        String replacedTip = replacements.get(packageTip);
        if(replacedTip != null && !replacedTip.equals(packageTip.tip)) {
            return compute(packageTip.replaceTip(replacedTip), replacements);
        }

        RepoTip repo = manifest.packageToRepo.get(packageTip);
        if(repo == null) {
            throw new IllegalArgumentException("No such package [tip]: " + packageTip);
        }
        RepoManifest repoManifest = manifest.repos.get(repo);
        PackageManifest packageManifest = repoManifest.packages.get(packageTip.name);

        ImmutableMap.Builder<PackageTip, String> replacementsNextBuilder = ImmutableMap.builder();
        replacementsNextBuilder.putAll(replacements);
        for(Map.Entry<PackageTip, String> e : packageManifest.replaceDeps.entrySet()) {
            if(!replacements.containsKey(e.getKey())) {
                replacementsNextBuilder.put(e);
            }
        }
        ImmutableMap<PackageTip, String> replacementsNext = replacementsNextBuilder.build();

        ImmutableMap.Builder<String, Pair<NormalDependencyType, SimpleRecursivePackageData<Result>>> dependencyResultsBuilder = ImmutableMap.builder();
        for(Map.Entry<String, Pair<NormalDependencyType, String>> e : packageManifest.normalDeps.entrySet()) {
            String dependencyName = e.getKey();
            NormalDependencyType dependencyType = e.getValue().getLeft();
            String dependencyTipName = e.getValue().getRight();
            SimpleRecursivePackageData<Result> dependencyResult;
            try {
                dependencyResult = compute(PackageTip.TYPE.of(dependencyName, dependencyTipName), replacementsNext);
            }
            catch(CycleException e1) {
                throw new CycleException(packageTip, replacements, e1);
            }
            dependencyResultsBuilder.put(dependencyName, Pair.of(dependencyType, dependencyResult));
        }
        Result result = new Result(key, repo, repoManifest, packageTip, packageManifest, replacementsNext);
        Map<String, Pair<NormalDependencyType, SimpleRecursivePackageData<Result>>> dependencyResults = dependencyResultsBuilder.build();
        return new SimpleRecursivePackageData<Result>(result, dependencyResults);
    }
}
