package qbt.map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.tip.PackageTip;

public abstract class DependencyComputer<M, V> {
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
    }

    private final Map<CacheKey, V> cache = Maps.newHashMap();

    private static class CycleException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private static String message(PackageTip packageTip, Map<PackageTip, String> replacements, CycleException next) {
            return (next == null ? "Cycle at" : "Via") + " " + packageTip + " with replacements " + replacements;
        }

        public CycleException(PackageTip packageTip, Map<PackageTip, String> replacements, CycleException next) {
            super(message(packageTip, replacements, next), next);
        }
    }

    public V compute(PackageTip packageTip) {
        return compute(packageTip, ImmutableMap.<PackageTip, String>of());
    }

    public V compute(PackageTip packageTip, Map<PackageTip, String> replacements) {
        CacheKey key = new CacheKey(packageTip, replacements);
        return compute(key);
    }

    public V compute(CacheKey key) {
        if(cache.containsKey(key)) {
            V result = cache.get(key);
            if(result == null) {
                throw new CycleException(key.packageTip, key.replacements, null);
            }
            return result;
        }
        cache.put(key, null);

        V result = computeUncached(key);
        cache.put(key, result);
        return result;
    }

    private V computeUncached(CacheKey key) {
        PackageTip packageTip = key.packageTip;
        Map<PackageTip, String> replacements = key.replacements;

        String replacedTip = replacements.get(packageTip);
        if(replacedTip != null && !replacedTip.equals(packageTip.tip)) {
            return compute(packageTip.replaceTip(replacedTip), replacements);
        }

        M intermediate = premap(packageTip);

        ImmutableMap.Builder<PackageTip, String> replacementsNextBuilder = ImmutableMap.builder();
        replacementsNextBuilder.putAll(replacements);
        for(Map.Entry<PackageTip, String> e : getReplaceDeps(intermediate, packageTip).entrySet()) {
            if(!replacements.containsKey(e.getKey())) {
                replacementsNextBuilder.put(e);
            }
        }
        Map<PackageTip, String> replacementsNext = replacementsNextBuilder.build();

        ImmutableMap.Builder<String, Pair<NormalDependencyType, V>> dependencyResultsBuilder = ImmutableMap.builder();
        for(Map.Entry<String, Pair<NormalDependencyType, String>> e : getNormalDeps(intermediate, packageTip).entrySet()) {
            String dependencyName = e.getKey();
            NormalDependencyType dependencyType = e.getValue().getLeft();
            String dependencyTipName = e.getValue().getRight();
            V dependencyResult;
            try {
                dependencyResult = compute(PackageTip.TYPE.of(dependencyName, dependencyTipName), replacementsNext);
            }
            catch(CycleException e1) {
                throw new CycleException(packageTip, replacements, e1);
            }
            dependencyResultsBuilder.put(dependencyName, Pair.of(dependencyType, dependencyResult));
        }
        Map<String, Pair<NormalDependencyType, V>> dependencyResults = dependencyResultsBuilder.build();
        return map(intermediate, new MapData<V>(packageTip, dependencyResults, replacementsNext));
    }

    public static final class MapData<V> {
        public final PackageTip packageTip;
        public final Map<String, Pair<NormalDependencyType, V>> dependencyResults;
        public final Map<PackageTip, String> replacements;

        private MapData(PackageTip packageTip, Map<String, Pair<NormalDependencyType, V>> dependencyResults, Map<PackageTip, String> replacements) {
            this.packageTip = packageTip;
            this.dependencyResults = dependencyResults;
            this.replacements = replacements;
        }
    }

    protected abstract M premap(PackageTip packageTip);
    protected abstract Map<String, Pair<NormalDependencyType, String>> getNormalDeps(M intermediate, PackageTip packageTip);
    protected abstract Map<PackageTip, String> getReplaceDeps(M intermediate, PackageTip packageTip);
    protected abstract V map(M intermediate, MapData<V> data);
}
