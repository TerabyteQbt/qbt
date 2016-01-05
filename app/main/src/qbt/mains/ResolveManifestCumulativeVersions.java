package qbt.mains;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import misc1.commons.options.OptionsResults;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.config.QbtConfig;
import qbt.manifest.current.QbtManifest;
import qbt.map.BuildCumulativeVersionComputer;
import qbt.map.CumulativeVersionComputer;
import qbt.map.CumulativeVersionComputerOptionsDelegate;
import qbt.map.CumulativeVersionComputerOptionsResult;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.PackageActionOptionsDelegate;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.tip.PackageTip;

public class ResolveManifestCumulativeVersions extends QbtCommand<ResolveManifestCumulativeVersions.Options> {
    @QbtCommandName("resolveManifestCumulativeVersions")
    public static interface Options extends QbtCommandOptions {
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final CumulativeVersionComputerOptionsDelegate<Options> cumulativeVersionComputerOptions = new CumulativeVersionComputerOptionsDelegate<Options>();
        public static final PackageActionOptionsDelegate<Options> packages = new PackageActionOptionsDelegate<Options>(PackageActionOptionsDelegate.NoArgsBehaviour.EMPTY);
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
        return null;
    }

    @Override
    public boolean isProgrammaticOutput() {
        return true;
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        QbtConfig config = Options.config.getConfig(options);
        QbtManifest manifest = Options.manifest.getResult(options).parse();
        Collection<PackageTip> packages = Options.packages.getPackages(config, manifest, options);
        final CumulativeVersionComputerOptionsResult cumulativeVersionComputerOptionsResult = Options.cumulativeVersionComputerOptions.getResults(options);

        CumulativeVersionComputer<?> cumulativeVersionComputer = new BuildCumulativeVersionComputer(config, manifest) {
            @Override
            protected Map<String, String> getQbtEnv() {
                return cumulativeVersionComputerOptionsResult.qbtEnv;
            }
        };
        for(PackageTip packageTip : packages) {
            CvRecursivePackageData<CumulativeVersionComputer.Result> r = cumulativeVersionComputer.compute(packageTip);
            System.out.println(packageTip + " " + r.v.getDigest().getRawDigest());
        }
        return 0;
    }
}
