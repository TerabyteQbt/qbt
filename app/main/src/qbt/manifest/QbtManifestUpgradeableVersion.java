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
