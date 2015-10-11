package qbt.vcs;

import java.nio.file.Path;

public class RawRemote {
    private final String remote;
    private final RawRemoteVcs vcs;

    public RawRemote(String remote, RawRemoteVcs vcs) {
        this.remote = remote;
        this.vcs = vcs;
    }

    public LocalVcs getLocalVcs() {
        return vcs.getLocalVcs();
    }

    public void addAsRemote(Path dir, String name) {
        vcs.addRemote(dir, name, remote);
    }

    public String getRemoteString() {
        return remote;
    }

    public RawRemoteVcs getRawRemoteVcs() {
        return vcs;
    }

    public void rsyncBranches(Path dir, String localPrefix, String remotePrefix) {
        vcs.rsyncBranches(dir, localPrefix, remote, remotePrefix);
    }

    @Override
    public String toString() {
        return "[" + remote + " via " + vcs + "]";
    }
}
