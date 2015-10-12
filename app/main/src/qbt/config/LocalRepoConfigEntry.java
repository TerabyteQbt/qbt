package qbt.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.PackageTip;
import qbt.VcsVersionDigest;
import qbt.repo.CommonRepoAccessor;
import qbt.repo.LocalRepoAccessor;
import qbt.vcs.LocalVcs;

public final class LocalRepoConfigEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRepoConfigEntry.class);

    public final LocalVcs localVcs;
    public final String format;

    public LocalRepoConfigEntry(LocalVcs localVcs, String format) {
        this.localVcs = localVcs;
        this.format = format;
    }

    public Path formatDirectory(PackageTip packageTip) {
        return Paths.get(format.replace("%r", packageTip.pkg).replace("%t", packageTip.tip));
    }

    public CommonRepoAccessor findRepo(final PackageTip repo, VcsVersionDigest version) {
        final Path repoDir = formatDirectory(repo);
        if(!localVcs.isRepo(repoDir)) {
            LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " missed");
            return null;
        }
        LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " hit");
        return new LocalRepoAccessor(localVcs, repoDir);
    }

    public LocalRepoAccessor findRepoLocal(final PackageTip repo) {
        final Path repoDir = formatDirectory(repo);
        if(!localVcs.isRepo(repoDir)) {
            LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " missed");
            return null;
        }
        LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " hit");
        return new LocalRepoAccessor(localVcs, repoDir);
    }
}
