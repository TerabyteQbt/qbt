package qbt.mains;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
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
import qbt.options.ManifestOptionsResult;
import qbt.options.RepoActionOptionsDelegate;
import qbt.repo.LocalRepoAccessor;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.Repository;

public final class UpdateManifestPlumbing extends QbtCommand<UpdateManifestPlumbing.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateManifestPlumbing.class);

    public static interface UpdateManifestCommonOptions {
        public static final OptionsLibrary<UpdateManifestCommonOptions> o = OptionsLibrary.of();
        public static final ConfigOptionsDelegate<UpdateManifestCommonOptions> config = new ConfigOptionsDelegate<UpdateManifestCommonOptions>();
        public static final ManifestOptionsDelegate<UpdateManifestCommonOptions> manifest = new ManifestOptionsDelegate<UpdateManifestCommonOptions>();
        public static final OptionsFragment<UpdateManifestCommonOptions, Boolean> allowNonFf = o.zeroArg("allow-non-ff").transform(o.flag()).helpDesc("Update even if the update is not fast-forward.");
        public static final OptionsFragment<UpdateManifestCommonOptions, Boolean> allowDirty = o.zeroArg("allow-dirty").transform(o.flag()).helpDesc("Update even if a repo is dirty");
    }

    @QbtCommandName("updateManifestPlumbing")
    public static interface Options extends UpdateManifestCommonOptions, QbtCommandOptions {
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
        return "update qbt-manifest file to match overrides (plumbing)";
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        return run(options, Options.repos);
    }

    public static <O extends UpdateManifestCommonOptions> int run(OptionsResults<? extends O> options, RepoActionOptionsDelegate<? super O> reposOption) throws IOException {
        QbtConfig config = UpdateManifestCommonOptions.config.getConfig(options);
        ManifestOptionsResult manifestResult = UpdateManifestCommonOptions.manifest.getResult(options);
        QbtManifest manifest = manifestResult.parse(config.manifestParser);
        Collection<RepoTip> repos = reposOption.getRepos(config, manifest, options);
        QbtManifest.Builder newManifest = manifest.builder();
        boolean fail = false;
        for(RepoTip repo : repos) {
            RepoManifest repoManifest = manifest.repos.get(repo);
            if(repoManifest == null) {
                throw new IllegalArgumentException("No such repo [tip]: " + repo);
            }
            VcsVersionDigest version = repoManifest.version.get();
            LocalRepoAccessor localRepoAccessor = config.localRepoFinder.findLocalRepo(repo);
            if(localRepoAccessor == null) {
                continue;
            }
            Repository repository = localRepoAccessor.vcs.getRepository(localRepoAccessor.dir);
            VcsVersionDigest newVersion = repository.getCurrentCommit();
            if(!repository.isClean()) {
                String prefix;
                String action;
                if(newVersion.equals(version)) {
                    prefix = repo + " is unchanged and dirty";
                    action = "ignoring";
                }
                else {
                    prefix = repo + " is changed (" + version.getRawDigest() + " -> " + newVersion.getRawDigest() + ") and dirty";
                    action = "updating anyway";
                }
                if(options.get(Options.allowDirty)) {
                    LOGGER.warn(prefix + ", " + action + "...");
                }
                else {
                    LOGGER.error(prefix + "!");
                    fail = true;
                    continue;
                }
            }
            if(!newVersion.equals(version)) {
                PinnedRepoAccessor pinnedAccessor = config.localPinsRepo.requirePin(repo, version);
                pinnedAccessor.findCommit(localRepoAccessor.dir);
                if(!options.get(Options.allowNonFf) && !repository.isAncestorOf(version, newVersion)) {
                    LOGGER.error("Updating " + repo + " from " + version.getRawDigest() + " to " + newVersion.getRawDigest() + " is not fast-forward!");
                    fail = true;
                    continue;
                }
                config.localPinsRepo.addPin(repo, localRepoAccessor.dir, newVersion);
                newManifest = newManifest.transform(repo, (rmb) -> rmb.set(RepoManifest.VERSION, Optional.of(newVersion)));
                LOGGER.info(String.format("Updated repo %s from %s to %s...", repo, version.getRawDigest(), newVersion.getRawDigest()));
            }
        }
        if(fail) {
            LOGGER.info("Some update(s) failed, not writing manifest!");
            return 1;
        }

        LOGGER.info("All update(s) successful, writing manifest.");
        manifestResult.deparse(config.manifestParser, newManifest.build());

        return 0;
    }
}
