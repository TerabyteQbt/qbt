package qbt.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import groovy.lang.GroovyShell;
import java.nio.file.Path;
import java.util.Optional;
import misc1.commons.ExceptionUtils;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.SimpleStructKey;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructBuilder;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import qbt.VcsVersionDigest;
import qbt.artifactcacher.ArtifactCacher;
import qbt.manifest.QbtManifestParser;
import qbt.manifest.current.CurrentQbtManifestParser;
import qbt.repo.CommonRepoAccessor;
import qbt.repo.LocalRepoAccessor;
import qbt.tip.RepoTip;

public final class QbtConfig extends Struct<QbtConfig, QbtConfig.Builder> {
    public final LocalRepoFinder localRepoFinder;
    public final LocalPinsRepo localPinsRepo;
    public final QbtRemoteFinder qbtRemoteFinder;
    public final ArtifactCacher artifactCacher;
    public final QbtManifestParser manifestParser;

    private QbtConfig(ImmutableMap<StructKey<QbtConfig, ?, ?>, Object> map) {
        super(TYPE, map);

        this.localRepoFinder = get(LOCAL_REPO_FINDER);
        this.localPinsRepo = get(LOCAL_PINS_REPO);
        this.qbtRemoteFinder = get(QBT_REMOTE_FINDER);
        this.artifactCacher = get(ARTIFACT_CACHER);
        this.manifestParser = get(MANIFEST_PARSER);
    }

    // Exists only for legacy pre-struct config files
    public QbtConfig(LocalRepoFinder localRepoFinder, LocalPinsRepo localPinsRepo, QbtRemoteFinder qbtRemoteFinder, ArtifactCacher artifactCacher) {
        this(constructMap(localRepoFinder, localPinsRepo, qbtRemoteFinder, artifactCacher));
    }

    private static ImmutableMap<StructKey<QbtConfig, ?, ?>, Object> constructMap(LocalRepoFinder localRepoFinder, LocalPinsRepo localPinsRepo, QbtRemoteFinder qbtRemoteFinder, ArtifactCacher artifactCacher) {
        Builder b = TYPE.builder();
        b = b.set(LOCAL_REPO_FINDER, localRepoFinder);
        b = b.set(LOCAL_PINS_REPO, localPinsRepo);
        b = b.set(QBT_REMOTE_FINDER, qbtRemoteFinder);
        b = b.set(ARTIFACT_CACHER, artifactCacher);
        return b.build().map;
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

    public CommonRepoAccessor requireCommonRepo(RepoTip repo, Optional<VcsVersionDigest> version) {
        LocalRepoAccessor local = localRepoFinder.findLocalRepo(repo);
        if(local != null) {
            return local;
        }
        if(!version.isPresent()) {
            throw new IllegalArgumentException("Could not find override " + repo);
        }
        return localPinsRepo.requirePin(repo, version.get(), "Could not find override or local pin for " + repo + " at " + version.get());
    }

    public static class Builder extends StructBuilder<QbtConfig, Builder> {
        public Builder(ImmutableSalvagingMap<StructKey<QbtConfig, ?, ?>, Object> map) {
            super(TYPE, map);
        }
    }

    public static final SimpleStructKey<QbtConfig, LocalRepoFinder> LOCAL_REPO_FINDER;
    public static final SimpleStructKey<QbtConfig, LocalPinsRepo> LOCAL_PINS_REPO;
    public static final SimpleStructKey<QbtConfig, QbtRemoteFinder> QBT_REMOTE_FINDER;
    public static final SimpleStructKey<QbtConfig, ArtifactCacher> ARTIFACT_CACHER;
    public static final SimpleStructKey<QbtConfig, QbtManifestParser> MANIFEST_PARSER;
    public static final StructType<QbtConfig, Builder> TYPE;
    static {
        ImmutableList.Builder<StructKey<QbtConfig, ?, ?>> b = ImmutableList.builder();

        b.add(LOCAL_REPO_FINDER = new SimpleStructKey<QbtConfig, LocalRepoFinder>("localRepoFinder"));
        b.add(LOCAL_PINS_REPO = new SimpleStructKey<QbtConfig, LocalPinsRepo>("localPinsRepo"));
        b.add(QBT_REMOTE_FINDER = new SimpleStructKey<QbtConfig, QbtRemoteFinder>("qbtRemoteFinder"));
        b.add(ARTIFACT_CACHER = new SimpleStructKey<QbtConfig, ArtifactCacher>("artifactCacher"));
        b.add(MANIFEST_PARSER = new SimpleStructKey<QbtConfig, QbtManifestParser>("manifestParser", new CurrentQbtManifestParser()));

        TYPE = new StructType<QbtConfig, Builder>(b.build(), QbtConfig::new, Builder::new);
    }
}
