package qbt.build;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.artifactcacher.ArtifactCacherUtils;
import qbt.artifactcacher.ArtifactReference;
import qbt.config.RepoConfig;
import qbt.map.CumulativeVersionComputer;
import qbt.metadata.Metadata;
import qbt.metadata.PackageMetadataType;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.cvrpd.CvRecursivePackageData;

public final class BuildData {
    public final CumulativeVersion v;
    public final RepoConfig.RequireRepoResult requireRepoResult;
    public final Metadata<PackageMetadataType> metadata;
    public final Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyArtifacts;

    private BuildData(CumulativeVersion v, RepoConfig.RequireRepoResult requireRepoResult, Metadata<PackageMetadataType> metadata, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyArtifacts) {
        this.v = v;
        this.requireRepoResult = requireRepoResult;
        this.metadata = metadata;
        this.dependencyArtifacts = dependencyArtifacts;
    }

    public BuildData(CvRecursivePackageData<CumulativeVersionComputer.Result> requireRepoResults, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyArtifacts) {
        this.v = requireRepoResults.v;
        this.requireRepoResult = requireRepoResults.result.getRight().requireRepoResult;
        this.metadata = requireRepoResults.result.getRight().packageManifest.metadata;
        this.dependencyArtifacts = dependencyArtifacts;
    }

    public BuildData pruneForCache() {
        Pair<CumulativeVersion, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>>> pruned = ArtifactCacherUtils.pruneForCache(v, dependencyArtifacts);
        return new BuildData(pruned.getLeft(), requireRepoResult, metadata, pruned.getRight());
    }
}
