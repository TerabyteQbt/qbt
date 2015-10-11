package qbt.vcs;

import java.nio.file.Path;
import java.util.Collection;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;

public interface CachedRemoteVcs {
    public RawRemoteVcs getRawRemoteVcs();
    public void findCommit(Path dir, String remote, Collection<VcsVersionDigest> versions);
    public boolean isRemote(String remote);
    public boolean commitExists(String remote, VcsVersionDigest version);
    public VcsTreeDigest getSubtree(String remote, VcsVersionDigest version, String subpath);
    public void checkoutTree(String remote, VcsVersionDigest version, String subpath, Path dir);
    public void addPin(Path dir, String remote, VcsVersionDigest commit);
    public int flushPins(String remote);
}
