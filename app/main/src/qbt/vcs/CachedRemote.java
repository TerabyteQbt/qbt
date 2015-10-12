package qbt.vcs;

import java.nio.file.Path;
import java.util.Collection;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;

public class CachedRemote extends RawRemote {
    private final String remote;
    private final CachedRemoteVcs vcs;

    public CachedRemote(String remote, CachedRemoteVcs vcs) {
        super(remote, vcs.getRawRemoteVcs());
        this.remote = remote;
        this.vcs = vcs;
    }

    public void findCommit(Path dir, Collection<VcsVersionDigest> versions) {
        vcs.findCommit(dir, remote, versions);
    }

    public void publishBranch(Path dir, VcsVersionDigest commit, String name) {
        vcs.getRawRemoteVcs().publishBranch(dir, remote, commit, name);
    }

    public void addPin(Path dir, VcsVersionDigest commit) {
        vcs.addPin(dir, remote, commit);
    }

    public int flushPins() {
        return vcs.flushPins(remote);
    }

    public VcsTreeDigest getSubtree(VcsVersionDigest version, String subpath) {
        return vcs.getSubtree(remote, version, subpath);
    }

    public void checkoutTree(VcsVersionDigest version, String subpath, Path dir) {
        vcs.checkoutTree(remote, version, subpath, dir);
    }

    public boolean matchedLocal(CachedRemote other) {
        return getLocalVcs().equals(other.getLocalVcs());
    }

    public boolean matchedRaw(CachedRemote other) {
        return getRawRemoteVcs().equals(other.getRawRemoteVcs());
    }

    @Override
    public String toString() {
        return "[" + remote + " via " + vcs + "]";
    }
}
