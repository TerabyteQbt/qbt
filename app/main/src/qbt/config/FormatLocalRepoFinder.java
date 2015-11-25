package qbt.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import qbt.tip.RepoTip;
import qbt.vcs.LocalVcs;

public class FormatLocalRepoFinder extends LocalRepoFinder {
    private final String format;

    public FormatLocalRepoFinder(LocalVcs vcs, String format) {
        super(vcs);
        this.format = format;
    }

    protected Path directory(RepoTip repo) {
        return Paths.get(format.replace("%r", repo.name).replace("%t", repo.tip));
    }
}
