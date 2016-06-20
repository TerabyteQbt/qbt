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
package qbt.manifest;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import qbt.manifest.current.RepoManifest;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public final class QbtManifestUtils {
    private QbtManifestUtils() {
        // nope
    }

    public static ImmutableMap<PackageTip, RepoTip> invertReposMap(ImmutableMap<RepoTip, RepoManifest> repos) {
        ImmutableMap.Builder<PackageTip, RepoTip> b = ImmutableMap.builder();
        for(Map.Entry<RepoTip, RepoManifest> e : repos.entrySet()) {
            for(String pkg : e.getValue().packages.keySet()) {
                b.put(e.getKey().toPackage(pkg), e.getKey());
            }
        }
        return b.build();
    }
}
