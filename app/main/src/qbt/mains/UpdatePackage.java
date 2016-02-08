package qbt.mains;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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
import qbt.manifest.current.PackageVerifyDeps;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.ManifestOptionsResult;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public final class UpdatePackage extends QbtCommand<UpdatePackage.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePackage.class);

    @QbtCommandName("updatePackage")
    public static interface Options extends QbtCommandOptions {
        public static final OptionsLibrary<Options> o = OptionsLibrary.of();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final OptionsFragment<Options, String> pkg = o.oneArg("package").transform(o.singleton()).helpDesc("Package to update");

        public final OptionsFragment<Options, String> prefix = o.oneArg("prefix").transform(o.singleton(null)).helpDesc("Prefix of package in the repository ('null' to make it empty)");

        public final OptionsFragment<Options, String> type = o.oneArg("buildType").transform(o.singleton(null)).transform((String h, String s) -> s != null ? s.toUpperCase() : null).helpDesc("Package type (normal, copy)");
        public final OptionsFragment<Options, Maybe<Boolean>> archIndependent = o.trinaryZero("archIndependent").transform(o.trinaryFlag()).helpDesc("Package is architecture independent");

        public final OptionsFragment<Options, ImmutableList<Pair<String, String>>> addNormalDeps = o.twoArg("addNormalDependency").helpDesc("Add Normal Dependencies (e.g. --addNormalDependency TYPE PKG)");
        public final OptionsFragment<Options, ImmutableList<Pair<String, String>>> removeNormalDeps = o.twoArg("removeNormalDependency").helpDesc("Remove Normal Dependencies (e.g. --removeNormalDependency TYPE PKG)");

        public final OptionsFragment<Options, ImmutableList<Pair<String, String>>> addVerifyDeps = o.twoArg("addVerifyDependency").helpDesc("Add Verify Dependencies (e.g. --addVerifyDependency TYPE PKG)");
        public final OptionsFragment<Options, ImmutableList<Pair<String, String>>> removeVerifyDeps = o.twoArg("removeVerifyDependency").helpDesc("Remove Verify Dependencies (e.g. --removeVerifyDependency TYPE PKG)");

        public final OptionsFragment<Options, ImmutableList<String>> addQbtEnv = o.oneArg("addQbtEnv").helpDesc("Add QBT environment variables (VAR=DEF_VALUE or just VAR for no default");
        public final OptionsFragment<Options, ImmutableList<String>> removeQbtEnv = o.oneArg("removeQbtEnv").helpDesc("Remove QBT environment variables");
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

        PackageTip pt = PackageTip.TYPE.parseRequire(options.get(Options.pkg));
        RepoTip rt = manifest.packageToRepo.get(pt);

        RepoManifest rm = manifest.repos.get(rt);
        PackageManifest pm = rm.packages.get(pt.name);

        if(!manifest.packageToRepo.containsKey(pt)) {
            throw new IllegalArgumentException("Package " + pt + " not found");
        }

        // Update metadata
        PackageMetadata.Builder pmd = pm.metadata.builder();

        String prefix = options.get(Options.prefix);
        if(prefix != null) {
            pmd = pmd.transform(PackageMetadata.PREFIX, (oldprefix) -> Maybe.of(prefix));
        }

        Maybe<Boolean> arch = options.get(Options.archIndependent);
        if(arch.isPresent()) {
            pmd = pmd.transform(PackageMetadata.ARCH_INDEPENDENT, (oldarch) -> arch.get(null));
        }

        String strType = options.get(Options.type);
        if(strType != null) {
            PackageBuildType type = PackageBuildType.valueOf(strType);
            pmd = pmd.transform(PackageMetadata.BUILD_TYPE, (oldtype) -> type);
        }

        for(String qbtEnv : options.get(Options.addQbtEnv)) {
            String[] parts = qbtEnv.split("=", 2);
            String key = parts[0];
            Maybe<String> val;
            if(parts.length > 1) {
                val = Maybe.of(parts[1]);
            }
            else {
                val = Maybe.not();
            }
            pmd = pmd.transform(PackageMetadata.QBT_ENV, (envMap) -> ImmutableMap.<String, Maybe<String>>builder().putAll(envMap).put(key, val).build());
        }

        for(String qbtEnv : options.get(Options.removeQbtEnv)) {
            pmd = pmd.transform(PackageMetadata.QBT_ENV, (envMap) -> ImmutableMap.<String, Maybe<String>>builder().putAll(Maps.filterEntries(envMap, (input) -> !input.getKey().equals(qbtEnv))).build());
        }

        // update normal deps
        PackageNormalDeps.Builder pnd = pm.get(PackageManifest.NORMAL_DEPS).builder();
        for(Pair<String, String> d : options.get(Options.addNormalDeps)) {
            NormalDependencyType t = NormalDependencyType.fromTag(d.getLeft());
            PackageTip dt = PackageTip.TYPE.parseRequire(d.getRight());
            Pair val = pnd.map.get(dt.name);
            if(val != null && val.equals(Pair.of(t, dt.tip))) {
                throw new IllegalArgumentException("Cannot add normal dependency, already exists: " + t + ":" + dt);
            }
            pnd = pnd.with(dt.name, Pair.of(t, dt.tip));
        }
        for(Pair<String, String> d : options.get(Options.removeNormalDeps)) {
            NormalDependencyType t = NormalDependencyType.fromTag(d.getLeft());
            PackageTip dt = PackageTip.TYPE.parseRequire(d.getRight());
            Pair val = pnd.map.get(dt.name);
            if(val == null || !val.equals(Pair.of(t, dt.tip))) {
                throw new IllegalArgumentException("Cannot remove normal dependency, already doesn't exist: " + t + ":" + dt);
            }
            pnd = pnd.without(dt.name);
        }

        // update verify deps
        PackageVerifyDeps.Builder pvd = pm.get(PackageManifest.VERIFY_DEPS).builder();
        for(Pair<String, String> d : options.get(Options.addVerifyDeps)) {
            String type = d.getLeft();
            PackageTip dt = PackageTip.TYPE.parseRequire(d.getRight());
            if(pvd.map.containsKey(Pair.of(dt, type))) {
                throw new IllegalArgumentException("Cannot add verify dependency, already exists: " + dt + "/" + d.getLeft());
            }
            pvd = pvd.with(Pair.of(dt, type), ObjectUtils.NULL);
        }
        for(Pair<String, String> d : options.get(Options.removeVerifyDeps)) {
            String type = d.getLeft();
            PackageTip dt = PackageTip.TYPE.parseRequire(d.getRight());
            if(!pvd.map.containsKey(Pair.of(dt, type))) {
                throw new IllegalArgumentException("Cannot remove verify dependency, already doesn't exist: " + dt + "/" + d.getLeft());
            }
            pvd = pvd.without(Pair.of(dt, type));
        }

        PackageManifest.Builder pmb = pm.builder().set(PackageManifest.METADATA, pmd).set(PackageManifest.NORMAL_DEPS, pnd).set(PackageManifest.VERIFY_DEPS, pvd);

        RepoManifest.Builder repoManifest = manifest.repos.get(rt).builder();
        repoManifest = repoManifest.transform(RepoManifest.PACKAGES, (pkgs) -> pkgs.with(pt.name, pmb));

        manifest = manifest.builder().with(rt, repoManifest).build();
        manifestResult.deparse(manifest);
        LOGGER.info("Package " + pt + " successfully updated and manifest written successfully");
        return 0;
    }
}
