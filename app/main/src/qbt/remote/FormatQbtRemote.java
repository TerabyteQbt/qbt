package qbt.remote;

import qbt.PackageTip;
import qbt.vcs.RawRemote;
import qbt.vcs.RawRemoteVcs;

public final class FormatQbtRemote extends AbstractQbtRemote {
    private final RawRemoteVcs vcs;
    private final String format;

    public FormatQbtRemote(RawRemoteVcs vcs, String format) {
        this.vcs = vcs;
        this.format = format;
    }

    private String formatRemote(PackageTip packageTip) {
        return format.replace("%r", packageTip.pkg).replace("%t", packageTip.tip);
    }

    @Override
    public RawRemote findRemote(PackageTip repo) {
        String remote = formatRemote(repo);
        return new RawRemote(remote, vcs);
    }
}
