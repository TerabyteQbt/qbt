package qbt.mains;

import java.io.IOException;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.config.QbtConfig;
import qbt.manifest.current.QbtManifest;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.ManifestOptionsResult;
import qbt.tip.RepoTip;

public final class RemoveRepository extends QbtCommand<RemoveRepository.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveRepository.class);

    @QbtCommandName("removeRepository")
    public static interface Options extends QbtCommandOptions {
        public static final OptionsLibrary<Options> o = OptionsLibrary.of();
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public final OptionsFragment<Options, String> repo = o.oneArg("repo").transform(o.singleton()).helpDesc("Repo to remove");
        public final OptionsFragment<Options, String> tip = o.oneArg("tip").transform(o.singleton("HEAD")).helpDesc("Tip to remove");
    }

    @Override
    public Class<Options> getOptionsClass() {
        return Options.class;
    }

    @Override
    public HelpTier getHelpTier() {
        return HelpTier.UNCOMMON;
    }

    @Override
    public String getDescription() {
        return "remove a repository from the manifest";
    }

    @Override
    public boolean isProgrammaticOutput() {
        return false;
    }

    @Override
    public int run(final OptionsResults<? extends Options> options) throws IOException {
        final QbtConfig config = Options.config.getConfig(options);
        final ManifestOptionsResult manifestResult = Options.manifest.getResult(options);
        QbtManifest manifest = manifestResult.parse();

        String repoName = options.get(Options.repo);
        String tip = options.get(Options.tip);

        RepoTip removeRepoTip = RepoTip.TYPE.of(repoName, tip);

        if(!manifest.repos.containsKey(removeRepoTip)) {
            throw new IllegalArgumentException("Repository tip " + removeRepoTip + " does not exist in manfiest");
        }

        manifest = manifest.builder().without(removeRepoTip).build();
        LOGGER.info("Removed repository " + removeRepoTip + " from manifest");
        // write out updated manifest
        manifestResult.deparse(manifest);
        return 0;
    }
}
