package qbt.mains;

import java.io.IOException;
import misc1.commons.options.OptionsResults;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.options.RepoActionOptionsDelegate;

public final class UpdateManifestPorcelain extends QbtCommand<UpdateManifestPorcelain.Options> {
    @QbtCommandName("updateManifest")
    public static interface Options extends UpdateManifestPlumbing.UpdateManifestCommonOptions, QbtCommandOptions {
        public static final RepoActionOptionsDelegate<Options> repos = new RepoActionOptionsDelegate<Options>(RepoActionOptionsDelegate.NoArgsBehaviour.OVERRIDES);
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
        return "update qbt-manifest file to match overrides";
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        return UpdateManifestPlumbing.run(options, Options.repos);
    }
}
