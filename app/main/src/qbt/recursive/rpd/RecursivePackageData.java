package qbt.recursive.rpd;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.rd.RecursiveData;

public abstract class RecursivePackageData<V, R extends RecursivePackageData<V, R>> extends RecursiveData<V, String, NormalDependencyType, R> {
    public RecursivePackageData(V result, Map<String, Pair<NormalDependencyType, R>> children) {
        super(result, children);
    }
}
