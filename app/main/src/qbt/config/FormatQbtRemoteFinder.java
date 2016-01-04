package qbt.config;

import qbt.remote.FormatQbtRemote;
import qbt.remote.QbtRemote;
import qbt.vcs.RawRemoteVcs;

public class FormatQbtRemoteFinder implements QbtRemoteFinder {
    private final RawRemoteVcs vcs;

    public FormatQbtRemoteFinder(RawRemoteVcs vcs) {
        this.vcs = vcs;
    }

    @Override
    public QbtRemote findQbtRemote(String remote) {
        return new FormatQbtRemote(vcs, remote);
    }
}
