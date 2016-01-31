package qbt.mains;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import misc1.commons.Maybe;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.NormalDependencyType;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.manifest.PackageBuildType;
import qbt.manifest.current.PackageManifest;
import qbt.manifest.current.PackageMetadata;
import qbt.manifest.current.PackageNormalDeps;
import qbt.manifest.current.PackageReplaceDeps;
import qbt.manifest.current.PackageVerifyDeps;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.ManifestOptionsResult;
import qbt.options.PackageManifestOptionsDelegate;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public final class AddPackage extends QbtCommand<AddPackage.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddPackage.class);

    @QbtCommandName("addPackage")
    public static interface Options extends QbtCommandOptions {
        public static final OptionsLibrary<Options> o = OptionsLibrary.of();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final PackageManifestOptionsDelegate<Options> packageManifest = new PackageManifestOptionsDelegate<Options>();
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

        String repoName = options.get(Options.repo);
        PackageTip pt = PackageTip.TYPE.parseRequire(options.get(Options.pkg));

        // Build metadata
        PackageMetadata.Builder pmd = PackageMetadata.TYPE.builder();
        if(options.get(Options.packageManifest.prefix) != null) {
            pmd = pmd.set(PackageMetadata.PREFIX, Maybe.of(options.get(Options.packageManifest.prefix)));
        }
        if(options.get(Options.packageManifest.arch)) {
            pmd = pmd.set(PackageMetadata.ARCH_INDEPENDENT, true);
        }
        if(options.get(Options.packageManifest.type) != null) {
            pmd = pmd.set(PackageMetadata.BUILD_TYPE, PackageBuildType.valueOf(options.get(Options.packageManifest.type)));
        }
        pmd = pmd.set(PackageMetadata.QBT_ENV, ImmutableSet.copyOf(options.get(Options.packageManifest.qbtEnv)));

        // build normal deps
        PackageNormalDeps.Builder pnd = PackageNormalDeps.TYPE.builder();
        for(Pair<String, Pair<NormalDependencyType, String>> d : Options.packageManifest.getNormalDependencies(options)) {
            pnd = pnd.with(d.getLeft(), d.getRight());
        }

        // build verify deps
        PackageVerifyDeps.Builder pvd = PackageVerifyDeps.TYPE.builder();
        for(Pair<PackageTip, String> d : Options.packageManifest.getVerifyDependencies(options)) {
            pvd = pvd.with(d, ObjectUtils.NULL);
        }

        // build replace deps
        PackageReplaceDeps.Builder prd = PackageReplaceDeps.TYPE.builder();
        for(Pair<PackageTip, String> d : Options.packageManifest.getReplaceDependencies(options)) {
            prd = prd.with(d.getLeft(), d.getRight());
        }

        PackageManifest.Builder pmb = PackageManifest.TYPE.builder();
        pmb = pmb.set(PackageManifest.METADATA, pmd);
        pmb = pmb.set(PackageManifest.NORMAL_DEPS, pnd);
        pmb = pmb.set(PackageManifest.VERIFY_DEPS, pvd);
        pmb = pmb.set(PackageManifest.REPLACE_DEPS, prd);

        RepoTip repo = RepoTip.TYPE.of(repoName, pt.tip);
        RepoManifest.Builder repoManifest = manifest.repos.get(repo).builder();
        repoManifest = repoManifest.set(RepoManifest.PACKAGES, repoManifest.get(RepoManifest.PACKAGES).with(pt.toString(), pmb));

        manifest = manifest.builder().with(repo, repoManifest).build();
        manifestResult.deparse(manifest);
        LOGGER.info("Added package " + pt + " and successfully wrote manifest");
        return 0;
    }
}
