package qbt.config;

import groovy.lang.GroovyShell;
import java.nio.file.Path;
import misc1.commons.ExceptionUtils;
import qbt.PackageTip;
import qbt.VcsVersionDigest;
import qbt.artifactcacher.ArtifactCacher;
import qbt.repo.CommonRepoAccessor;
import qbt.repo.LocalRepoAccessor;
import qbt.repo.RemoteRepoAccessor;

public final class QbtConfig {
    public final LocalRepoFinder localRepoFinder;
    public final RepoConfig repoConfig;
    public final ArtifactCacher artifactCacher;

    public QbtConfig(LocalRepoFinder localRepoFinder, RepoConfig repoConfig, ArtifactCacher artifactCacher) {
        this.localRepoFinder = localRepoFinder;
        this.repoConfig = repoConfig;
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
        RemoteRepoAccessor remote = repoConfig.findRemoteRepo(repo, version);
        if(remote != null) {
            return remote;
        }
        throw new IllegalArgumentException("Could not find local or remote repo for " + repo + " at " + version);
    }
}
