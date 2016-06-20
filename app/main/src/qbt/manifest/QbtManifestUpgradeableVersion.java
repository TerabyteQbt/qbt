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

import qbt.manifest.current.QbtManifest;

abstract class QbtManifestUpgradeableVersion<M, B, N> extends QbtManifestVersion<M, B> {
    final Class<N> upgradeManifestClass;
    final QbtManifestVersion<N, ?> nextVersion;

    QbtManifestUpgradeableVersion(int version, QbtManifestVersion<N, ?> nextVersion, Class<M> manifestClass, Class<N> upgradeManifestClass) {
        super(version, manifestClass);
        this.upgradeManifestClass = upgradeManifestClass;
        this.nextVersion = nextVersion;
    }

    @Override
    public final QbtManifest current(M manifest) {
        return nextVersion.current(upgrade(manifest));
    }

    public abstract N upgrade(M manifest);
}
