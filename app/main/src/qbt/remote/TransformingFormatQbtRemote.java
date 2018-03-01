package qbt.remote;

import java.util.function.Function;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;
import qbt.vcs.RawRemoteVcs;

public final class TransformingFormatQbtRemote implements QbtRemote {
    private final RawRemoteVcs vcs;
    private final String format;
    private final Function<String, String> transformation;

    public TransformingFormatQbtRemote(RawRemoteVcs vcs, Function<String, String> transformation, String format) {
        this.vcs = vcs;
        this.format = format;
        this.transformation = transformation;
    }

    private String formatRemote(RepoTip repo) {
        return format.replace("%r", transformation.apply(repo.name)).replace("%t", repo.tip);
    }

    @Override
    public RawRemote findRemote(RepoTip repo, boolean autoVivify) {
        String remote = formatRemote(repo);
        RawRemote rawRemote = new RawRemote(remote, vcs);

        if(vcs.remoteExists(remote)) {
            return rawRemote;
        }
        if(!autoVivify) {
            // doesn't exist and we weren't asked to create it
            return null;
        }
        // If autoVivify asked for, which FormatQbtRemote doesn't support, return the RawRemote and let the fireworks happen elsewhere.
        return rawRemote;
    }
}
