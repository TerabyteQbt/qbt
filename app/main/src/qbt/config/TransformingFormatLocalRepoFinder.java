package qbt.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import qbt.tip.RepoTip;
import qbt.vcs.LocalVcs;

public class TransformingFormatLocalRepoFinder extends SimpleLocalRepoFinder {
    private final String format;
    private final Function<String, String> transformation;

    public TransformingFormatLocalRepoFinder(LocalVcs vcs, Function<String, String> transformation, String format) {
        super(vcs);
        this.format = format;
        this.transformation = transformation;
    }

    @Override
    protected Path directory(RepoTip repo) {
        return Paths.get(format.replace("%r", this.transformation.apply(repo.name)).replace("%t", repo.tip));
    }
}
