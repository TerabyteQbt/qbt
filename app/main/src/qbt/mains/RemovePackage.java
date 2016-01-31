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
import qbt.manifest.current.RepoManifest;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.ManifestOptionsResult;
import qbt.tip.PackageTip;

public final class RemovePackage extends QbtCommand<RemovePackage.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemovePackage.class);

    @QbtCommandName("removePackage")
    public static interface Options extends QbtCommandOptions {
        public static final OptionsLibrary<Options> o = OptionsLibrary.of();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final OptionsFragment<Options, String> pkg = o.oneArg("package").transform(o.singleton()).helpDesc("Package to remove");
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
        return "remove a package from the manifest";
    }

    @Override
    public boolean isProgrammaticOutput() {
        return false;
    }

    @Override
    public int run(final OptionsResults<? extends Options> options) throws IOException {
        final ManifestOptionsResult manifestResult = Options.manifest.getResult(options);
        QbtManifest manifest = manifestResult.parse();
        PackageTip pt = PackageTip.TYPE.parseRequire(options.get(Options.pkg));

        manifest = manifest.builder().transform(manifest.packageToRepo.get(pt), (RepoManifest.Builder rm) -> {
            return rm.set(RepoManifest.PACKAGES, rm.get(RepoManifest.PACKAGES).without(pt.name));
        }).build();
        manifestResult.deparse(manifest);
        LOGGER.info("Removed package " + pt + " and successfully wrote manifest");
        return 0;
    }
}
