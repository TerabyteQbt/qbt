package qbt.recursive.rpd;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.rd.RecursiveDataCanonicalizer;

public abstract class RecursivePackageDataCanonicalizer<V, R extends RecursivePackageData<V, R>, K> extends RecursiveDataCanonicalizer<V, String, NormalDependencyType, R, K> {
    @Override
    protected abstract R newR(V result, Map<String, Pair<NormalDependencyType, R>> dependencyResults);
}
