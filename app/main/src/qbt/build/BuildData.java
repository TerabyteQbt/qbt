//   Copyright 2016 Keith Amling
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
package qbt.build;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.artifactcacher.ArtifactReference;
import qbt.manifest.current.PackageMetadata;
import qbt.map.CumulativeVersionComputer;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.repo.CommonRepoAccessor;

public final class BuildData {
    public final CumulativeVersion v;
    public final CommonRepoAccessor commonRepoAccessor;
    public final PackageMetadata metadata;
    public final Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyArtifacts;

    public BuildData(CvRecursivePackageData<CumulativeVersionComputer.Result> commonRepoAccessor, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyArtifacts) {
        this.v = commonRepoAccessor.v;
        this.commonRepoAccessor = commonRepoAccessor.result.getRight().commonRepoAccessor;
        this.metadata = commonRepoAccessor.result.getRight().packageManifest.metadata;
        this.dependencyArtifacts = dependencyArtifacts;
    }
}
