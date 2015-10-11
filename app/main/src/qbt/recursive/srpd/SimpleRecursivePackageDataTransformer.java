package qbt.recursive.srpd;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.rpd.RecursivePackageDataTransformer;

public abstract class SimpleRecursivePackageDataTransformer<V1, V2> extends RecursivePackageDataTransformer<V1, SimpleRecursivePackageData<V1>, V2, SimpleRecursivePackageData<V2>> {
    @Override
    protected SimpleRecursivePackageData<V2> transformResult(SimpleRecursivePackageData<V1> r, Map<String, Pair<NormalDependencyType, SimpleRecursivePackageData<V2>>> dependencyResults) {
        return new SimpleRecursivePackageData<V2>(transformResult(r.result, dependencyResults), dependencyResults);
    }

    protected abstract V2 transformResult(V1 result, Map<String, Pair<NormalDependencyType, SimpleRecursivePackageData<V2>>> dependencyResults);
}
