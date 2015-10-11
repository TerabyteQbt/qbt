package qbt.mains;

import misc1.commons.options.OptionsResults;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.options.PackageActionOptionsDelegate;

public class BuildPorcelain extends QbtCommand<BuildPorcelain.Options> {
    @QbtCommandName("build")
    public static interface Options extends BuildPlumbing.BuildCommonOptions, QbtCommandOptions {
        public static final PackageActionOptionsDelegate<Options> packages = new PackageActionOptionsDelegate<Options>(PackageActionOptionsDelegate.NoArgsBehaviour.THROW);
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
        return "build packages";
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws Exception {
        return BuildPlumbing.run(options, Options.packages);
    }
}
