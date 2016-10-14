package qbt.config;

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.QbtUtils;
import qbt.repo.LocalRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.LocalVcs;

public abstract class SimpleLocalRepoFinder implements LocalRepoFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleLocalRepoFinder.class);

    private final LocalVcs vcs;

    public SimpleLocalRepoFinder(LocalVcs vcs) {
        this.vcs = vcs;
    }

    @Override
    public LocalRepoAccessor findLocalRepo(RepoTip repo) {
        final Path repoDir = directory(repo);
        if(!vcs.isRepo(repoDir)) {
            LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " missed");
            return null;
        }
        LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " hit");
        return new LocalRepoAccessor(vcs, repoDir);
    }

    @Override
    public LocalRepoAccessor createLocalRepo(RepoTip repo) {
        final Path repoDir = directory(repo);
        if(repoDir.toFile().exists()) {
            throw new IllegalArgumentException("Local repo for " + repo + " already exists in " + repoDir);
        }
        QbtUtils.mkdirs(repoDir);
        vcs.createWorkingRepo(repoDir);
        return new LocalRepoAccessor(vcs, repoDir);
    }

    protected abstract Path directory(RepoTip repo);
}
