package qbt.vcs;

import java.nio.file.Path;
import qbt.VcsVersionDigest;

public interface RawRemoteVcs {
    public LocalVcs getLocalVcs();
    public void fetchPins(Path dir, String remote);
    public void addPinToRemote(Path dir, String remote, VcsVersionDigest commit);
    public boolean remoteExists(String remote);
}
