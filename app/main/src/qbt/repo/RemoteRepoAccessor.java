package qbt.repo;

import java.nio.file.Path;
import misc1.commons.Maybe;
import qbt.PackageDirectory;
import qbt.QbtTempDir;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.repo.CommonRepoAccessor;
import qbt.vcs.CachedRemote;

public final class RemoteRepoAccessor implements CommonRepoAccessor {
    private final CachedRemote remote;
    private final VcsVersionDigest version;

    public RemoteRepoAccessor(CachedRemote remote, VcsVersionDigest version) {
        this.remote = remote;
        this.version = version;
    }

    @Override
    public PackageDirectory makePackageDirectory(String prefix) {
        final QbtTempDir packageDir = new QbtTempDir();
        // We could leak packageDir if this checkout crashes but oh
        // well.
        remote.checkoutTree(version, prefix, packageDir.path);
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

    @Override
    public VcsTreeDigest getEffectiveTree(Maybe<String> prefix) {
        if(prefix.isPresent()) {
            return remote.getSubtree(version, prefix.get(null));
        }
        else {
            return remote.getLocalVcs().emptyTree();
        }
    }

    @Override
    public boolean isOverride() {
        return false;
    }
}
