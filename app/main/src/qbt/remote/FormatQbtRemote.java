package qbt.remote;

import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;
import qbt.vcs.RawRemoteVcs;

public final class FormatQbtRemote extends AbstractQbtRemote {
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
    public RawRemote findRemote(RepoTip repo) {
        String remote = formatRemote(repo);
        return new RawRemote(remote, vcs);
    }
}
