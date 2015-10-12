package qbt.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.PackageTip;
import qbt.QbtUtils;
import qbt.repo.LocalRepoAccessor;
import qbt.vcs.LocalVcs;

public final class FormatLocalRepoFinder implements LocalRepoFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormatLocalRepoFinder.class);

    private final LocalVcs vcs;
    private final String format;

    public FormatLocalRepoFinder(LocalVcs vcs, String format) {
        this.vcs = vcs;
        this.format = format;
    }

    public Path formatDirectory(PackageTip packageTip) {
        return Paths.get(format.replace("%r", packageTip.pkg).replace("%t", packageTip.tip));
    }

    @Override
    public LocalRepoAccessor findLocalRepo(PackageTip repo) {
        final Path repoDir = formatDirectory(repo);
        if(!vcs.isRepo(repoDir)) {
            LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " missed");
            return null;
        }
        LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " hit");
        return new LocalRepoAccessor(vcs, repoDir);
    }

    @Override
    public LocalRepoAccessor createLocalRepo(PackageTip repo) {
        final Path repoDir = formatDirectory(repo);
        if(repoDir.toFile().exists()) {
            throw new IllegalArgumentException("Local repo for " + repo + " already exists in " + repoDir);
        }
        QbtUtils.mkdirs(repoDir);
        vcs.createWorkingRepo(repoDir);
        return new LocalRepoAccessor(vcs, repoDir);
    }
}