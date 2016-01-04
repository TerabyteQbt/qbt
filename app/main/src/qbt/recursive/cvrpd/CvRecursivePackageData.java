package qbt.recursive.cvrpd;

import com.google.common.base.Function;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.rpd.RecursivePackageData;

public class CvRecursivePackageData<V> extends RecursivePackageData<Pair<CumulativeVersion, V>, CvRecursivePackageData<V>> {
    public final CumulativeVersion v;

    public CvRecursivePackageData(CumulativeVersion v, V result, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V>>> children) {
        this(Pair.of(v, result), children);
    }

    public CvRecursivePackageData(Pair<CumulativeVersion, V> result, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V>>> children) {
        super(result, children);
        this.v = result.getLeft();
    }

    public static <V> Function<CvRecursivePackageData<V>, V> innerValueFunction() {
        return (input) -> input.result.getRight();
    }
}
