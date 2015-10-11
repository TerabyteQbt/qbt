package qbt.mains;

import java.io.IOException;
import misc1.commons.options.OptionsResults;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.options.RepoActionOptionsDelegate;

public final class RunOverridesPorcelain extends QbtCommand<RunOverridesPorcelain.Options> {
    @QbtCommandName("runOverrides")
    public static interface Options extends RunOverridesPlumbing.RunOverridesCommonOptions, QbtCommandOptions {
        public static final RepoActionOptionsDelegate<Options> repos = new RepoActionOptionsDelegate<Options>(RepoActionOptionsDelegate.NoArgsBehaviour.OVERRIDES);
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
        return "run a command in all overrides";
    }

    @Override
    public boolean isProgrammaticOutput() {
        return true;
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        return RunOverridesPlumbing.run(options, Options.repos);
    }
}
