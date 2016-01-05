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
