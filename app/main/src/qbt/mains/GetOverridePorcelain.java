package qbt.mains;

import misc1.commons.options.OptionsResults;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.options.RepoActionOptionsDelegate;

public final class GetOverridePorcelain extends QbtCommand<GetOverridePorcelain.Options> {
    @QbtCommandName("getOverride")
    public static interface Options extends GetOverridePlumbing.GetOverrideCommonOptions, QbtCommandOptions {
        public static final RepoActionOptionsDelegate<Options> repos = new RepoActionOptionsDelegate<Options>(RepoActionOptionsDelegate.NoArgsBehaviour.THROW);
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
        return "check out a repository, setting it up as an override";
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws Exception {
        return GetOverridePlumbing.run(options, Options.repos);
    }
}
