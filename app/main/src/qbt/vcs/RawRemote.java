package qbt.vcs;

public class RawRemote {
    private final String remote;
    private final RawRemoteVcs vcs;

    public RawRemote(String remote, RawRemoteVcs vcs) {
        this.remote = remote;
        this.vcs = vcs;
    }

    public String getRemoteString() {
        return remote;
    }

    public RawRemoteVcs getRawRemoteVcs() {
        return vcs;
    }

    @Override
    public String toString() {
        return "[" + remote + " via " + vcs + "]";
    }
}
