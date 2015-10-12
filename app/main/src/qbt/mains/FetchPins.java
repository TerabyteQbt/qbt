package qbt.mains;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Collection;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import misc1.commons.options.UnparsedOptionsFragment;
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
import qbt.remote.QbtRemote;
import qbt.vcs.RawRemote;

public class FetchPins extends QbtCommand<FetchPins.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FetchPins.class);

    @QbtCommandName("fetchPins")
    public static interface Options extends QbtCommandOptions {
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final RepoActionOptionsDelegate<Options> repos = new RepoActionOptionsDelegate<Options>(RepoActionOptionsDelegate.NoArgsBehaviour.THROW);
        public static final OptionsFragment<Options, ?, ImmutableList<String>> remote = new UnparsedOptionsFragment<Options>("QBT remote from which to fetch", false, 1, 1);
    }

    @Override
    public Class<Options> getOptionsClass() {
        return Options.class;
    }

    @Override
    public HelpTier getHelpTier() {
        return HelpTier.COMMON;
    }

    @Override
    public String getDescription() {
        return "Fetch pins from remote qbt repositories";
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        QbtConfig config = Options.config.getConfig(options);
        QbtManifest manifest = Options.manifest.getResult(options).parse();
        Collection<PackageTip> repos = Options.repos.getRepos(config, manifest, options);
        String qbtRemoteString = Iterables.getOnlyElement(options.get(Options.remote));
        QbtRemote qbtRemote = config.qbtRemoteFinder.requireQbtRemote(qbtRemoteString);
        boolean fail = false;
        for(PackageTip repo : repos) {
            RepoManifest repoManifest = manifest.repos.get(repo);
            if(repoManifest == null) {
                throw new IllegalArgumentException("No such repo [tip]: " + repo);
            }
            VcsVersionDigest version = repoManifest.version;

            if(config.localPinsRepo.findPin(repo, version) != null) {
                LOGGER.debug("[" + repo + "] Already have " + version);
                continue;
            }

            RawRemote remote = qbtRemote.requireRemote(repo);

            LOGGER.info("[" + repo + "] Fetching from " + remote + "...");

            config.localPinsRepo.fetchPins(repo, remote);

            if(config.localPinsRepo.findPin(repo, version) == null) {
                LOGGER.error("[" + repo + "] But did not find " + version + "!");
                fail = true;
            }
        }
        return fail ? 1 : 0;
    }
}
