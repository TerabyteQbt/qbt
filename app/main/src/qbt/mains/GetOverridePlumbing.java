package qbt.mains;

import com.google.common.collect.ImmutableList;
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
import qbt.config.RepoConfig;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.RepoActionOptionsDelegate;
import qbt.repo.LocalRepoAccessor;
import qbt.vcs.CachedRemote;
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
            RepoConfig.RequireRepoRemoteResult requireRepoRemoteResult = config.repoConfig.requireRepoRemote(repo, version);
            LocalRepoAccessor newLocal = config.repoConfig.createLocalRepo(repo);
            if(newLocal == null) {
                throw new IllegalArgumentException("Requested override of " + repo + " which has no associated local directory");
            }
            CachedRemote remote = requireRepoRemoteResult.getRemote();
            LocalVcs localVcs = remote.getLocalVcs();
            if(!localVcs.equals(newLocal.vcs)) {
                throw new IllegalStateException("Mismatch of local VCS between remote " + remote + " and local " + newLocal.vcs);
            }

            remote.addAsRemote(newLocal.dir, "origin");
            remote.getRawRemoteVcs().fetchRemote(newLocal.dir, "origin");
            remote.findCommit(newLocal.dir, ImmutableList.of(version));
            localVcs.getRepository(newLocal.dir).checkout(version);

            LOGGER.info("Overrode " + repo + " from " + remote.getRemoteString() + " to " + newLocal.dir + " at " + version.getRawDigest() + ".");
        }
        return 0;
    }
}
