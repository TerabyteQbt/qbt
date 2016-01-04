package qbt.recursive.cvrpd;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.cv.CumulativeVersionNodeData;
import qbt.recursive.rpd.RecursivePackageDataTransformer;

public abstract class CvRecursivePackageDataTransformer<V1, V2> extends RecursivePackageDataTransformer<Pair<CumulativeVersion, V1>, CvRecursivePackageData<V1>, Pair<CumulativeVersion, V2>, CvRecursivePackageData<V2>> {
    @Override
    protected boolean keepLink(Pair<CumulativeVersion, V1> result, String dependencyName, NormalDependencyType dependencyType, CvRecursivePackageData<V1> dependencyResult) {
        return keepLink(result.getLeft(), result.getRight(), dependencyName, dependencyType, dependencyResult);
    }

    private final Function<Pair<NormalDependencyType, CvRecursivePackageData<V2>>, Pair<NormalDependencyType, CumulativeVersion>> getDependencyVersion = (input) -> Pair.of(input.getLeft(), input.getRight().v);
    @Override
    protected CvRecursivePackageData<V2> transformResult(CvRecursivePackageData<V1> r, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V2>>> dependencyResults) {
        CumulativeVersion v = CumulativeVersion.of(transformNodeData(r.v.result), Maps.transformValues(dependencyResults, getDependencyVersion));
        return new CvRecursivePackageData<V2>(v, transformResult(r.v, v, r.result.getRight(), dependencyResults), dependencyResults);
    }

    protected CumulativeVersionNodeData transformNodeData(CumulativeVersionNodeData nodeData) {
        return nodeData;
    }

    protected boolean keepLink(CumulativeVersion v, V1 result, String dependencyName, NormalDependencyType dependencyType, CvRecursivePackageData<V1> dependencyResult) {
        return true;
    }

    protected abstract V2 transformResult(CumulativeVersion vOld, CumulativeVersion vNew, V1 result, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V2>>> dependencyResults);
}
