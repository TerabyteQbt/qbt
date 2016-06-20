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

import com.google.common.collect.ImmutableSet;
import misc1.commons.merge.Merge;
import qbt.VcsVersionDigest;
import qbt.manifest.v2.QbtManifest;
import qbt.manifest.v2.RepoManifest;
import qbt.manifest.v2.Upgrades;
import qbt.tip.RepoTip;

class V2QbtManifestVersion extends QbtManifestUpgradeableVersion<QbtManifest, QbtManifest.Builder, qbt.manifest.current.QbtManifest> {
    public V2QbtManifestVersion(V3QbtManifestVersion nextVersion) {
        super(2, nextVersion, QbtManifest.class, qbt.manifest.current.QbtManifest.class);
    }

    @Override
    public ImmutableSet<RepoTip> getRepos(QbtManifest manifest) {
        return manifest.map.keySet();
    }

    @Override
    public QbtManifest.Builder builder(QbtManifest manifest) {
        return manifest.builder();
    }

    @Override
    public QbtManifest.Builder withRepoVersion(QbtManifest.Builder builder, RepoTip repo, VcsVersionDigest commit) {
        return builder.transform(repo, (repoManifest) -> repoManifest.set(RepoManifest.VERSION, commit));
    }

    @Override
    public QbtManifest.Builder withoutRepo(QbtManifest.Builder builder, RepoTip repo) {
        return builder.without(repo);
    }

    @Override
    public QbtManifest build(QbtManifest.Builder builder) {
        return builder.build();
    }

    @Override
    public Merge<QbtManifest> merge() {
        return QbtManifest.TYPE.merge();
    }

    @Override
    public QbtManifestParser<QbtManifest> parser() {
        return new JsonQbtManifestParser<QbtManifest, QbtManifest.Builder>(this) {
            @Override
            protected JsonSerializer<QbtManifest.Builder> serializer() {
                return QbtManifest.SERIALIZER;
            }
        };
    }

    @Override
    public qbt.manifest.current.QbtManifest upgrade(QbtManifest manifest) {
        return Upgrades.upgrade_QbtManifest(manifest).build();
    }
}
