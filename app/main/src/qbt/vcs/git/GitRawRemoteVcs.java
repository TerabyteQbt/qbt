package qbt.vcs.git;

import java.nio.file.Path;
import qbt.VcsVersionDigest;
import qbt.vcs.LocalVcs;
import qbt.vcs.simple.SimpleRawRemoteVcs;

public final class GitRawRemoteVcs extends SimpleRawRemoteVcs {
    @Override
    public LocalVcs getLocalVcs() {
        return new GitLocalVcs();
    }

    @Override
    public void fetchPins(Path dir, String remote) {
        GitUtils.fetchPins(dir, remote);
    }

    @Override
    public void addPinToRemote(Path dir, String remote, VcsVersionDigest commit) {
        GitUtils.addPinToRemote(dir, remote, commit);
    }

    @Override
    public boolean remoteExists(String remote) {
        return GitUtils.remoteExists(remote);
    }
}
