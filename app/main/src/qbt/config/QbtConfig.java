package qbt.config;

import groovy.lang.GroovyShell;
import java.nio.file.Path;
import misc1.commons.ExceptionUtils;
import qbt.artifactcacher.ArtifactCacher;

public final class QbtConfig {
    public final RepoConfig repoConfig;
    public final ArtifactCacher artifactCacher;

    public QbtConfig(RepoConfig repoConfig, ArtifactCacher artifactCacher) {
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
}
