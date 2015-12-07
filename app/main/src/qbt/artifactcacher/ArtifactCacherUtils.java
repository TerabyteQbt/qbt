package qbt.artifactcacher;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.recursive.cvrpd.CvRecursivePackageDataTransformer;

public class ArtifactCacherUtils {
    private ArtifactCacherUtils() {
        // no
    }

    public static <V> CvRecursivePackageData<V> pruneForCache(CvRecursivePackageData<V> r) {
        return new CvRecursivePackageDataTransformer<V, V>() {
            @Override
            protected boolean keepLink(CumulativeVersion v, V result, String dependencyName, NormalDependencyType dependencyType, CvRecursivePackageData<V> dependencyResult) {
                return dependencyType != NormalDependencyType.PHANTOM;
            }

            @Override
            protected V transformResult(CumulativeVersion vOld, CumulativeVersion vNew, V result, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V>>> dependencyResults) {
                return result;
            }
        }.transform(r);
    }

    public static <V> Pair<CumulativeVersion, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V>>>> pruneForCache(CumulativeVersion v, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V>>> dependencyResults) {
        CvRecursivePackageData<V> pruned = pruneForCache(new CvRecursivePackageData<V>(v, null, dependencyResults));
        return Pair.of(pruned.v, pruned.children);
    }
}
