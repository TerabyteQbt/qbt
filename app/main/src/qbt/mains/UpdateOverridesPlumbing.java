package qbt.mains;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import misc1.commons.options.NamedBooleanFlagOptionsFragment;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.VcsVersionDigest;
import qbt.config.QbtConfig;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.RepoActionOptionsDelegate;
import qbt.repo.LocalRepoAccessor;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.LocalVcs;
import qbt.vcs.Repository;

public final class UpdateOverridesPlumbing extends QbtCommand<UpdateOverridesPlumbing.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateOverridesPlumbing.class);

    public static interface UpdateOverridesCommonOptions {
        public static final ConfigOptionsDelegate<UpdateOverridesCommonOptions> config = new ConfigOptionsDelegate<UpdateOverridesCommonOptions>();
        public static final ManifestOptionsDelegate<UpdateOverridesCommonOptions> manifest = new ManifestOptionsDelegate<UpdateOverridesCommonOptions>();
        public static final OptionsFragment<UpdateOverridesCommonOptions, ?, Boolean> allowDirty = new NamedBooleanFlagOptionsFragment<UpdateOverridesCommonOptions>(ImmutableList.of("--allow-dirty"), "Attempt update even if a repo is dirty");
    }

    @QbtCommandName("updateOverridesPlumbing")
    public static interface Options extends UpdateOverridesCommonOptions, QbtCommandOptions {
        public static final RepoActionOptionsDelegate<Options> repos = new RepoActionOptionsDelegate<Options>(RepoActionOptionsDelegate.NoArgsBehaviour.EMPTY);
    }

    @Override
    public Class<Options> getOptionsClass() {
        return Options.class;
    }

    @Override
    public HelpTier getHelpTier() {
        return HelpTier.PLUMBING;
    }

    @Override
    public String getDescription() {
        return "update overrides to match qbt-manifest file (plumbing)";
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        return run(options, Options.repos);
    }

    public static <O extends UpdateOverridesCommonOptions> int run(OptionsResults<? extends O> options, RepoActionOptionsDelegate<? super O> reposOption) throws IOException {
        QbtConfig config = Options.config.getConfig(options);
        QbtManifest manifest = Options.manifest.getResult(options).parse();
        Collection<RepoTip> repos = reposOption.getRepos(config, manifest, options);
        boolean fail = false;
        for(RepoTip repo : repos) {
            RepoManifest repoManifest = manifest.repos.get(repo);
            if(repoManifest == null) {
                throw new IllegalArgumentException("No such repo [tip]: " + repo);
            }
            VcsVersionDigest version = repoManifest.version;
            LocalRepoAccessor localRepoAccessor = config.localRepoFinder.findLocalRepo(repo);
            if(localRepoAccessor == null) {
                continue;
            }
            Path dir = localRepoAccessor.dir;
            LocalVcs localVcs = localRepoAccessor.vcs;
            Repository repository = localVcs.getRepository(dir);
            VcsVersionDigest oldVersion = repository.getCurrentCommit();
            if(oldVersion.equals(version)) {
                continue;
            }
            if(!repository.isClean()) {
                String prefix = repo + " needs a change (" + oldVersion.getRawDigest() + " -> " + version.getRawDigest() + ") and is dirty";
                if(options.get(Options.allowDirty)) {
                    LOGGER.warn(prefix + ", will attempt update...");
                }
                else {
                    LOGGER.error(prefix + "!");
                    fail = true;
                    continue;
                }
            }
            if(!repository.commitExists(version)) {
                PinnedRepoAccessor pinnedAccessor = config.localPinsRepo.requirePin(repo, version);
                pinnedAccessor.findCommit(dir);
            }
            repository.checkout(version);
            LOGGER.info("Updated " + repo + " from " + oldVersion.getRawDigest() + " to " + version.getRawDigest());
        }
        if(fail) {
            LOGGER.info("Some update(s) failed");
            return 1;
        }
        LOGGER.info("All update(s) successful");

        return 0;
    }
}
