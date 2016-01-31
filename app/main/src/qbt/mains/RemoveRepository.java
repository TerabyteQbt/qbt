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
import qbt.manifest.current.QbtManifest;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.ManifestOptionsResult;
import qbt.tip.RepoTip;

public final class RemoveRepository extends QbtCommand<RemoveRepository.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveRepository.class);

    @QbtCommandName("removeRepository")
    public static interface Options extends QbtCommandOptions {
        public static final OptionsLibrary<Options> o = OptionsLibrary.of();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public final OptionsFragment<Options, String> repo = o.oneArg("repo").transform(o.singleton()).helpDesc("Repository to remove");
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
        final ManifestOptionsResult manifestResult = Options.manifest.getResult(options);
        QbtManifest manifest = manifestResult.parse();

        RepoTip rt = RepoTip.TYPE.parseRequire(options.get(Options.repo));

        if(!manifest.repos.containsKey(rt)) {
            throw new IllegalArgumentException("Repository tip " + rt + " does not exist in manfiest");
        }

        manifest = manifest.builder().without(rt).build();
        // write out updated manifest
        manifestResult.deparse(manifest);
        LOGGER.info("Removed repository " + rt + " and successfully wrote manifest");
        return 0;
    }
}
