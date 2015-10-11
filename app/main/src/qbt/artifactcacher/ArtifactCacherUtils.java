package qbt.artifactcacher;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.metadata.PackageMetadataType;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.cv.CumulativeVersionNodeData;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.recursive.cvrpd.CvRecursivePackageDataTransformer;

public class ArtifactCacherUtils {
    private ArtifactCacherUtils() {
        // no
    }

    public static <V> CvRecursivePackageData<V> pruneForCache(CvRecursivePackageData<V> r) {
        return new CvRecursivePackageDataTransformer<V, V>() {
            @Override
            protected CumulativeVersionNodeData transformNodeData(CumulativeVersionNodeData nodeData) {
                Map<String, String> qbtEnv0 = nodeData.qbtEnv;
                Set<String> qbtEnvKeep = nodeData.metadata.get(PackageMetadataType.QBT_ENV);
                Map<String, String> qbtEnv1 = Maps.filterKeys(qbtEnv0, Predicates.in(qbtEnvKeep));
                return nodeData.builder().withQbtEnv(qbtEnv1).build();
            }

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
