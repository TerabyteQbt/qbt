package qbt.mains;

import java.io.IOException;
import misc1.commons.options.OptionsResults;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.options.RepoActionOptionsDelegate;

public final class UpdateOverridesPorcelain extends QbtCommand<UpdateOverridesPorcelain.Options> {
    @QbtCommandName("updateOverrides")
    public static interface Options extends UpdateOverridesPlumbing.UpdateOverridesCommonOptions, QbtCommandOptions {
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
        return "update overrides to match qbt-manifest file";
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        return UpdateOverridesPlumbing.run(options, Options.repos);
    }
}
