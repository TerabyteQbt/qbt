package qbt.build;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.artifactcacher.ArtifactReference;
import qbt.map.CumulativeVersionComputer;
import qbt.metadata.Metadata;
import qbt.metadata.PackageMetadataType;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.repo.CommonRepoAccessor;

public final class BuildData {
    public final CumulativeVersion v;
    public final CommonRepoAccessor commonRepoAccessor;
    public final Metadata<PackageMetadataType> metadata;
    public final Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyArtifacts;

    public BuildData(CvRecursivePackageData<CumulativeVersionComputer.Result> commonRepoAccessor, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyArtifacts) {
        this.v = commonRepoAccessor.v;
        this.commonRepoAccessor = commonRepoAccessor.result.getRight().commonRepoAccessor;
        this.metadata = commonRepoAccessor.result.getRight().packageManifest.metadata;
        this.dependencyArtifacts = dependencyArtifacts;
    }
}
