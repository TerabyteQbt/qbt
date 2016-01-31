package qbt.repo;

import java.nio.file.Path;
import misc1.commons.Maybe;
import qbt.PackageDirectory;
import qbt.QbtTempDir;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.vcs.LocalVcs;
import qbt.vcs.RawRemote;
import qbt.vcs.RawRemoteVcs;
import qbt.vcs.Repository;

public final class SimplePinnedRepoAccessor implements PinnedRepoAccessor {
    private final RawRemoteVcs vcs;
    private final Path cache;
    private final Repository cacheRepo;
    private final VcsVersionDigest version;

    public SimplePinnedRepoAccessor(RawRemoteVcs vcs, Path cache, VcsVersionDigest version) {
        this.vcs = vcs;
        this.cache = cache;
        this.cacheRepo = vcs.getLocalVcs().getRepository(cache);
        this.version = version;
    }

    @Override
    public PackageDirectory makePackageDirectory(String prefix) {
        final QbtTempDir packageDir = new QbtTempDir();
        // We could leak packageDir if this checkout crashes but oh
        // well.
        cacheRepo.checkoutTree(getSubtree(prefix), packageDir.path);
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
            return cacheRepo.getSubtree(version, prefix.get(null));
        }
        else {
            return vcs.getLocalVcs().emptyTree();
        }
    }

    @Override
    public VcsTreeDigest getSubtree(VcsTreeDigest tree, String subpath) {
        return cacheRepo.getSubtree(tree, subpath);
    }

    @Override
    public boolean isOverride() {
        return false;
    }

    @Override
    public void findCommit(Path dir) {
        vcs.addPinToRemote(cache, dir.toAbsolutePath().toString(), version);
    }

    @Override
    public LocalVcs getLocalVcs() {
        return vcs.getLocalVcs();
    }

    @Override
    public void addPin(Path dir, VcsVersionDigest version) {
        vcs.addPinToRemote(dir, cache.toAbsolutePath().toString(), version);
    }

    @Override
    public VcsTreeDigest getSubtree(String prefix) {
        return cacheRepo.getSubtree(version, prefix);
    }

    @Override
    public void pushToRemote(RawRemote remote) {
        RawRemoteVcs vcs2 = remote.getRawRemoteVcs();
        if(!vcs2.equals(vcs)) {
            throw new IllegalStateException("Mismatch of VCS between pins " + vcs + " and remote " + vcs2);
        }
        vcs.addPinToRemote(cache, remote.getRemoteString(), version);
    }

    @Override
    public boolean versionExists() {
        return vcs.getLocalVcs().getRepository(cache).commitExists(version);
    }
}
