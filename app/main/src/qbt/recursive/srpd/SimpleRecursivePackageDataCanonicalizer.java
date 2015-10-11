package qbt.recursive.srpd;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.rpd.RecursivePackageDataCanonicalizer;

public abstract class SimpleRecursivePackageDataCanonicalizer<V, K> extends RecursivePackageDataCanonicalizer<V, SimpleRecursivePackageData<V>, K> {
    @Override
    protected SimpleRecursivePackageData<V> newR(V result, Map<String, Pair<NormalDependencyType, SimpleRecursivePackageData<V>>> dependencyResults) {
        return new SimpleRecursivePackageData<V>(result, dependencyResults);
    }
}
