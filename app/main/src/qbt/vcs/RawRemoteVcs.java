package qbt.vcs;

import java.nio.file.Path;
import qbt.VcsVersionDigest;

public interface RawRemoteVcs {
    public LocalVcs getLocalVcs();
    public void addRemote(Path dir, String name, String remote);
    public void fetchRemote(Path dir, String name);
    public boolean isRemoteRaw(String remote);
    public void fetchPins(Path dir, String remote);
    public void addPinToRemote(Path dir, String remote, VcsVersionDigest commit);
    public void addLocalPinToRemote(Path dir, String remote, VcsVersionDigest commit);
    public int flushLocalPinsToRemote(Path dir, String remote);
    public void publishBranch(Path dir, String remote, VcsVersionDigest commit, String name);
    public String getName();
    public void rsyncBranches(Path dir, String localPrefix, String remote, String remotePrefix);
    public boolean remoteExists(String remote);
}
