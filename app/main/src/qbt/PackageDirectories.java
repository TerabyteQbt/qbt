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
package qbt;

import java.nio.file.Path;
import misc1.commons.Maybe;
import qbt.build.BuildData;
import qbt.manifest.current.PackageMetadata;
import qbt.map.CumulativeVersionComputer;
import qbt.repo.CommonRepoAccessor;

public final class PackageDirectories {
    private PackageDirectories() {
        // no
    }

    public static PackageDirectory forBuildData(BuildData bd) {
        return forCommon(bd.metadata, bd.commonRepoAccessor);
    }

    public static PackageDirectory forCvcResult(CumulativeVersionComputer.Result result) {
        return forCommon(result.packageManifest.metadata, result.commonRepoAccessor);
    }

    private static PackageDirectory forCommon(PackageMetadata metadata, CommonRepoAccessor commonRepoAccessor) {
        Maybe<String> prefix = metadata.get(PackageMetadata.PREFIX);
        if(prefix.isPresent()) {
            return commonRepoAccessor.makePackageDirectory(prefix.get(null));
        }
        else {
            final QbtTempDir packageDir = new QbtTempDir();
            return new PackageDirectory() {
                @Override
                public Path getDir() {
                    return packageDir.path;
                }

                @Override
                public void close() {
                    packageDir.close();
                }
            };
        }
    }
}
