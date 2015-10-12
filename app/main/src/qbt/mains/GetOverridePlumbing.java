package qbt.mains;

import java.io.IOException;
import java.util.Collection;
import misc1.commons.options.OptionsResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.PackageTip;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.QbtManifest;
import qbt.RepoManifest;
import qbt.VcsVersionDigest;
import qbt.config.QbtConfig;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.RepoActionOptionsDelegate;
import qbt.repo.LocalRepoAccessor;
import qbt.repo.PinnedRepoAccessor;
import qbt.vcs.LocalVcs;

public final class GetOverridePlumbing extends QbtCommand<GetOverridePlumbing.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetOverridePlumbing.class);

    public static interface GetOverrideCommonOptions {
        public static final ConfigOptionsDelegate<GetOverrideCommonOptions> config = new ConfigOptionsDelegate<GetOverrideCommonOptions>();
        public static final ManifestOptionsDelegate<GetOverrideCommonOptions> manifest = new ManifestOptionsDelegate<GetOverrideCommonOptions>();
    }

    @QbtCommandName("getOverridePlumbing")
    public static interface Options extends GetOverrideCommonOptions, QbtCommandOptions {
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
        return "check out a repository, setting it up as an override (plumbing)";
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        return run(options, Options.repos);
    }

    public static <O extends GetOverrideCommonOptions> int run(OptionsResults<? extends O> options, RepoActionOptionsDelegate<? super O> reposOption) throws IOException {
        QbtConfig config = GetOverrideCommonOptions.config.getConfig(options);
        QbtManifest manifest = GetOverrideCommonOptions.manifest.getResult(options).parse();
        Collection<PackageTip> repos = reposOption.getRepos(config, manifest, options);
        for(PackageTip repo : repos) {
            RepoManifest repoManifest = manifest.repos.get(repo);
            if(repoManifest == null) {
                throw new IllegalArgumentException("No such repo [tip] " + repo);
            }
            VcsVersionDigest version = repoManifest.version;
            PinnedRepoAccessor pinnedAccessor = config.localPinsRepo.requirePin(repo, version);
            LocalRepoAccessor newLocal = config.localRepoFinder.createLocalRepo(repo);
            if(newLocal == null) {
                throw new IllegalArgumentException("Requested override of " + repo + " which has no associated local directory");
            }
            LocalVcs localVcs = pinnedAccessor.getLocalVcs();
            if(!localVcs.equals(newLocal.vcs)) {
                throw new IllegalStateException("Mismatch of local VCS between pins " + localVcs + " and local " + newLocal.vcs);
            }

            pinnedAccessor.findCommit(newLocal.dir);
            localVcs.getRepository(newLocal.dir).checkout(version);

            LOGGER.info("Overrode " + repo + " to " + newLocal.dir + " at " + version.getRawDigest() + ".");
        }
        return 0;
    }
}
