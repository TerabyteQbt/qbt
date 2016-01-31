package qbt.mains;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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

public final class UpdatePackage extends QbtCommand<UpdatePackage.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePackage.class);

    @QbtCommandName("updatePackage")
    public static interface Options extends QbtCommandOptions {
        public static final OptionsLibrary<Options> o = OptionsLibrary.of();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final PackageManifestOptionsDelegate<Options> packageManifest = new PackageManifestOptionsDelegate<Options>();
        public static final OptionsFragment<Options, String> pkg = o.oneArg("package").transform(o.singleton()).helpDesc("Package to update");
        public final OptionsFragment<Options, Boolean> removeFields = o.zeroArg("removeFields").transform(o.flag()).helpDesc("Remove fields instead of adding them");
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
        return "update a package's metadata in the manifest";
    }

    @Override
    public boolean isProgrammaticOutput() {
        return false;
    }

    @Override
    public int run(final OptionsResults<? extends Options> options) throws IOException {
        final ManifestOptionsResult manifestResult = Options.manifest.getResult(options);
        QbtManifest manifest = manifestResult.parse();

        Boolean removeFields = options.get(Options.removeFields);
        PackageTip pt = PackageTip.TYPE.parseRequire(options.get(Options.pkg));
        RepoTip rt = manifest.packageToRepo.get(pt);

        RepoManifest rm = manifest.repos.get(rt);
        PackageManifest pm = rm.packages.get(pt.name);

        // Update metadata
        PackageMetadata.Builder pmd = pm.metadata.builder();
        if(options.get(Options.packageManifest.prefix) != null) {
            pmd = pmd.set(PackageMetadata.PREFIX, Maybe.of(options.get(Options.packageManifest.prefix)));
        }

        // TODO: update ARCH? Ignoring that field for now, it is annoying to determine whether we should change the value or not since it is just a flag.

        if(options.get(Options.packageManifest.type) != null) {
            pmd = pmd.set(PackageMetadata.BUILD_TYPE, PackageBuildType.valueOf(options.get(Options.packageManifest.type)));
        }
        for(String qbtEnv : options.get(Options.packageManifest.qbtEnv)) {
            if(removeFields) {
                pmd = pmd.set(PackageMetadata.QBT_ENV, ImmutableSet.copyOf(Iterables.filter(pmd.get(PackageMetadata.QBT_ENV), (String s) -> !s.equals(qbtEnv))));
                continue;
            }
            pmd = pmd.set(PackageMetadata.QBT_ENV, ImmutableSet.copyOf(Iterables.concat(pmd.get(PackageMetadata.QBT_ENV), ImmutableSet.of(qbtEnv))));
        }

        // update normal deps
        PackageNormalDeps.Builder pnd = pm.get(PackageManifest.NORMAL_DEPS).builder();
        for(Pair<String, Pair<NormalDependencyType, String>> d : Options.packageManifest.getNormalDependencies(options)) {
            if(removeFields) {
                pnd = pnd.without(d.getLeft());
                continue;
            }
            pnd = pnd.with(d.getLeft(), d.getRight());
        }

        // update verify deps
        PackageVerifyDeps.Builder pvd = pm.get(PackageManifest.VERIFY_DEPS).builder();
        for(Pair<PackageTip, String> d : Options.packageManifest.getVerifyDependencies(options)) {
            if(removeFields) {
                pvd = pvd.without(d);
                continue;
            }
            pvd = pvd.with(d, ObjectUtils.NULL);
        }

        // update replace deps
        PackageReplaceDeps.Builder prd = pm.get(PackageManifest.REPLACE_DEPS).builder();
        for(Pair<PackageTip, String> d : Options.packageManifest.getReplaceDependencies(options)) {
            if(removeFields) {
                prd = prd.without(d.getLeft());
                continue;
            }
            prd = prd.with(d.getLeft(), d.getRight());
        }

        PackageManifest.Builder pmb = PackageManifest.TYPE.builder();
        pmb = pmb.set(PackageManifest.METADATA, pmd);
        pmb = pmb.set(PackageManifest.NORMAL_DEPS, pnd);
        pmb = pmb.set(PackageManifest.VERIFY_DEPS, pvd);
        pmb = pmb.set(PackageManifest.REPLACE_DEPS, prd);

        RepoManifest.Builder repoManifest = manifest.repos.get(rt).builder();
        repoManifest = repoManifest.set(RepoManifest.PACKAGES, repoManifest.get(RepoManifest.PACKAGES).with(pt.toString(), pmb));

        manifest = manifest.builder().with(rt, repoManifest).build();
        manifestResult.deparse(manifest);
        return 0;
    }
}
