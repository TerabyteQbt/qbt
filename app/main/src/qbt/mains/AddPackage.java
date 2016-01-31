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
import qbt.manifest.current.PackageManifest;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.ManifestOptionsResult;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public final class AddPackage extends QbtCommand<AddPackage.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddPackage.class);

    @QbtCommandName("addPackage")
    public static interface Options extends QbtCommandOptions {
        public static final OptionsLibrary<Options> o = OptionsLibrary.of();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final OptionsFragment<Options, String> repo = o.oneArg("repo").transform(o.singleton()).helpDesc("Repo to add package in");
        public static final OptionsFragment<Options, String> pkg = o.oneArg("package").transform(o.singleton()).helpDesc("Package to add");
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
        return "add a package to the manifest";
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
        PackageTip pt = PackageTip.TYPE.parseRequire(options.get(Options.pkg));

        if(manifest.packageToRepo.containsKey(pt)) {
            throw new IllegalArgumentException("Package " + pt + " already exists in repo " + rt);
        }
        PackageManifest.Builder pmb = PackageManifest.TYPE.builder();
        RepoManifest.Builder repoManifest = manifest.repos.get(rt).builder();
        repoManifest = repoManifest.transform(RepoManifest.PACKAGES, (pkgs) -> pkgs.with(pt.toString(), pmb));

        manifest = manifest.builder().with(rt, repoManifest).build();
        manifestResult.deparse(manifest);
        LOGGER.info("Package " + pt + " added to repo " + rt + " and manifest written successfully");
        return 0;
    }
}
