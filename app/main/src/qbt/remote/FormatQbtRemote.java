package qbt.remote;

import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;
import qbt.vcs.RawRemoteVcs;

public final class FormatQbtRemote implements QbtRemote {
    private final RawRemoteVcs vcs;
    private final String format;

    public FormatQbtRemote(RawRemoteVcs vcs, String format) {
        this.vcs = vcs;
        this.format = format;
    }

    private String formatRemote(RepoTip repo) {
        return format.replace("%r", repo.name).replace("%t", repo.tip);
    }

    @Override
    public RawRemote findRemote(RepoTip repo, boolean autoVivify) {
        String remote = formatRemote(repo);
        if(!vcs.remoteExists(remote) && !autoVivify) {
            return null;
        }
        // If autoVivify asked for, which FormatQbtRemote doesn't support, return the RawRemote and let the fireworks happen elsewhere.
        return new RawRemote(remote, vcs);
    }
}
