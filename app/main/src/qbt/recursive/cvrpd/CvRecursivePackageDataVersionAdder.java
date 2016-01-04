package qbt.recursive.cvrpd;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.cv.CumulativeVersionNodeData;
import qbt.recursive.rpd.RecursivePackageData;
import qbt.recursive.rpd.RecursivePackageDataTransformer;

public abstract class CvRecursivePackageDataVersionAdder<V, R extends RecursivePackageData<V, R>> extends RecursivePackageDataTransformer<V, R, Pair<CumulativeVersion, V>, CvRecursivePackageData<V>> {
    private final Function<Pair<NormalDependencyType, CvRecursivePackageData<V>>, Pair<NormalDependencyType, CumulativeVersion>> simplify = (input) -> Pair.of(input.getLeft(), input.getRight().v);

    @Override
    protected CvRecursivePackageData<V> transformResult(R r, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V>>> dependencyResults) {
        CumulativeVersion v = CumulativeVersion.of(nodeData(r.result), Maps.transformValues(dependencyResults, simplify));
        return new CvRecursivePackageData<V>(v, r.result, dependencyResults);
    }

    protected abstract CumulativeVersionNodeData nodeData(V result);
}
