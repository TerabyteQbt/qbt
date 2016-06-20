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
import qbt.manifest.current.QbtManifest;
import qbt.tip.RepoTip;

public abstract class QbtManifestVersion<M, B> {
    final int version;
    final Class<M> manifestClass;

    QbtManifestVersion(int version, Class<M> manifestClass) {
        this.version = version;
        this.manifestClass = manifestClass;
    }

    public QbtManifestVersion<?, ?> max(QbtManifestVersion<?, ?> other) {
        if(other.version > version) {
            return other;
        }
        return this;
    }

    public abstract ImmutableSet<RepoTip> getRepos(M manifest);
    public abstract QbtManifest current(M manifest);
    public abstract B builder(M manifest);

    public abstract B withRepoVersion(B builder, RepoTip repo, VcsVersionDigest commit);
    public abstract B withoutRepo(B builder, RepoTip repo);
    public abstract M build(B builder);

    public abstract Merge<M> merge();
    public abstract QbtManifestParser<M> parser();
}
