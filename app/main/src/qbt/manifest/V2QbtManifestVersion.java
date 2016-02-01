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
