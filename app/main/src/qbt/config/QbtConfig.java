package qbt.config;

import groovy.lang.GroovyShell;
import java.nio.file.Path;
import misc1.commons.ExceptionUtils;
import qbt.PackageTip;
import qbt.VcsVersionDigest;
import qbt.artifactcacher.ArtifactCacher;
import qbt.repo.CommonRepoAccessor;
import qbt.repo.LocalRepoAccessor;
import qbt.repo.PinnedRepoAccessor;

public final class QbtConfig {
    public final LocalRepoFinder localRepoFinder;
    public final LocalPinsRepo localPinsRepo;
    public final QbtRemoteFinder qbtRemoteFinder;
    public final ArtifactCacher artifactCacher;

    public QbtConfig(LocalRepoFinder localRepoFinder, LocalPinsRepo localPinsRepo, QbtRemoteFinder qbtRemoteFinder, ArtifactCacher artifactCacher) {
        this.localRepoFinder = localRepoFinder;
        this.localPinsRepo = localPinsRepo;
        this.qbtRemoteFinder = qbtRemoteFinder;
        this.artifactCacher = artifactCacher;
    }

    public static QbtConfig parse(Path f) {
        GroovyShell shell = new GroovyShell();
        shell.setVariable("workspaceRoot", f.getParent());
        try {
            return (QbtConfig) shell.evaluate(f.toFile());
        }
        catch(Exception e) {
            throw ExceptionUtils.commute(e);
        }
    }

    public CommonRepoAccessor requireCommonRepo(PackageTip repo, VcsVersionDigest version) {
        LocalRepoAccessor local = localRepoFinder.findLocalRepo(repo);
        if(local != null) {
            return local;
        }
        PinnedRepoAccessor localPin = localPinsRepo.findPin(repo, version);
        if(localPin != null) {
            return localPin;
        }
        throw new IllegalArgumentException("Could not find override or local pin for " + repo + " at " + version);
    }
}
