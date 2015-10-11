package qbt.recursive.rpd;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.rd.RecursiveDataTransformer;

public abstract class RecursivePackageDataTransformer<V1, R1 extends RecursivePackageData<V1, R1>, V2, R2 extends RecursivePackageData<V2, R2>> extends RecursiveDataTransformer<String, NormalDependencyType, V1, R1, V2, R2> {
    @Override
    protected boolean keepLink(V1 result, String dependencyName, NormalDependencyType dependencyType, R1 dependencyResult) {
        return super.keepLink(result, dependencyName, dependencyType, dependencyResult);
    }

    @Override
    protected abstract R2 transformResult(R1 r, Map<String, Pair<NormalDependencyType, R2>> dependencyResults);
}
