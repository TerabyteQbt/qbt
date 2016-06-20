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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import qbt.VcsVersionDigest;
import qbt.manifest.current.QbtManifest;
import qbt.tip.RepoTip;

public final class LegacyQbtManifest<M, B> {
    public final QbtManifestVersion<M, B> version;
    public final M manifest;

    public LegacyQbtManifest(QbtManifestVersion<M, B> version, M manifest) {
        this.version = version;
        this.manifest = manifest;
    }

    public ImmutableSet<RepoTip> getRepos() {
        return version.getRepos(manifest);
    }

    public ImmutableList<String> deparse() {
        return version.parser().deparse(manifest);
    }

    public LegacyQbtManifest.Builder<M, B> builder() {
        return new LegacyQbtManifest.Builder<M, B>(version, version.builder(manifest));
    }

    public QbtManifest current() {
        return version.current(manifest);
    }

    public <M2, B2> LegacyQbtManifest<M2, B2> upgrade(QbtManifestVersion<M2, B2> targetVersion) {
        if(version == targetVersion) {
            return (LegacyQbtManifest<M2, B2>)this;
        }
        if(version instanceof QbtManifestUpgradeableVersion) {
            return new Object() {
                public <N> LegacyQbtManifest<M2, B2> run(QbtManifestUpgradeableVersion<M, B, N> version) {
                    N nextManifest = version.upgrade(manifest);
                    QbtManifestVersion<N, ?> nextVersion = version.nextVersion;
                    return new Object() {
                        public <X> LegacyQbtManifest<M2, B2> run(QbtManifestVersion<N, X> nextVersion) {
                            return new LegacyQbtManifest<N, X>(nextVersion, nextManifest).upgrade(targetVersion);
                        }
                    }.run(nextVersion);
                }
            }.run((QbtManifestUpgradeableVersion<M, B, ?>)version);
        }
        throw new IllegalArgumentException();
    }

    public static final class Builder<M, B> {
        public final QbtManifestVersion<M, B> version;
        public final B builder;

        public Builder(QbtManifestVersion<M, B> version, B builder) {
            this.version = version;
            this.builder = builder;
        }

        public Builder<M, B> withRepoVersion(RepoTip repo, VcsVersionDigest commit) {
            return new Builder<M, B>(version, version.withRepoVersion(builder, repo, commit));
        }

        public Builder<M, B> withoutRepo(RepoTip repo) {
            return new Builder<M, B>(version, version.withoutRepo(builder, repo));
        }

        public LegacyQbtManifest<M, B> build() {
            return new LegacyQbtManifest<M, B>(version, version.build(builder));
        }
    }
}
