package qbt.recursive.srpd;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.rpd.RecursivePackageData;

public class SimpleRecursivePackageData<V> extends RecursivePackageData<V, SimpleRecursivePackageData<V>> {
    public SimpleRecursivePackageData(V result, Map<String, Pair<NormalDependencyType, SimpleRecursivePackageData<V>>> dependencyResults) {
        super(result, dependencyResults);
    }
}
