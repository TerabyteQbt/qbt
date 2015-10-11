package qbt.mains;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import misc1.commons.options.NamedBooleanFlagOptionsFragment;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import qbt.HelpTier;
import qbt.PackageTip;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.QbtManifest;
import qbt.artifactcacher.ArtifactCacherUtils;
import qbt.config.QbtConfig;
import qbt.map.BuildManifestCumulativeVersionComputer;
import qbt.map.CumulativeVersionComputer;
import qbt.map.CumulativeVersionComputerOptionsDelegate;
import qbt.map.CumulativeVersionComputerOptionsResult;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.PackageActionOptionsDelegate;
import qbt.recursive.cvrpd.CvRecursivePackageData;

public class ResolveManifestCumulativeVersions extends QbtCommand<ResolveManifestCumulativeVersions.Options> {
    @QbtCommandName("resolveManifestCumulativeVersions")
    public static interface Options extends QbtCommandOptions {
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final CumulativeVersionComputerOptionsDelegate<Options> cumulativeVersionComputerOptions = new CumulativeVersionComputerOptionsDelegate<Options>();
        public static final PackageActionOptionsDelegate<Options> packages = new PackageActionOptionsDelegate<Options>(PackageActionOptionsDelegate.NoArgsBehaviour.EMPTY);
        public static final OptionsFragment<Options, ?, Boolean> pruneForCache = new NamedBooleanFlagOptionsFragment<Options>(ImmutableList.of("--pruneForCache"), "Prune cumulative versions to those used for the cache");
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

        CumulativeVersionComputer<?> cumulativeVersionComputer = new BuildManifestCumulativeVersionComputer(config, manifest) {
            @Override
            protected Map<String, String> getQbtEnv() {
                return cumulativeVersionComputerOptionsResult.qbtEnv;
            }
        };
        for(PackageTip packageTip : packages) {
            CvRecursivePackageData<CumulativeVersionComputer.Result> r = cumulativeVersionComputer.compute(packageTip);
            if(options.get(Options.pruneForCache)) {
                r = ArtifactCacherUtils.pruneForCache(r);
            }
            System.out.println(packageTip + " " + r.v.getDigest().getRawDigest());
        }
        return 0;
    }
}
