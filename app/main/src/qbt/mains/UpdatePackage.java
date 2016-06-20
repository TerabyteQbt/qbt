//   Copyright 2016 Keith Amling
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
package qbt.mains;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Map;
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

        public final OptionsFragment<Options, String> prefix = o.oneArg("prefix").transform(o.singleton(null)).helpDesc("Prefix of package in the repository");

        public final OptionsFragment<Options, String> type = o.oneArg("buildType").transform(o.singleton(null)).helpDesc("Package type (normal, copy)");
        public final OptionsFragment<Options, Maybe<Boolean>> archIndependent = o.trinary("archIndependent").helpDesc("Package is architecture independent");

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
        if(!manifest.packageToRepo.containsKey(pt)) {
            throw new IllegalArgumentException(("Package " + pt + " does not exist"));
        }
        RepoTip rt = manifest.packageToRepo.get(pt);

        RepoManifest rm = manifest.repos.get(rt);
        PackageManifest pm = rm.packages.get(pt.name);

        // Update metadata
        PackageMetadata.Builder pmd = pm.metadata.builder();

        String prefix = options.get(Options.prefix);
        if(prefix != null) {
            pmd = pmd.set(PackageMetadata.PREFIX, Maybe.of(prefix));
        }

        Maybe<Boolean> arch = options.get(Options.archIndependent);
        if(arch.isPresent()) {
            pmd = pmd.set(PackageMetadata.ARCH_INDEPENDENT, arch.get(null));
        }

        String strType = options.get(Options.type);
        if(strType != null) {
            PackageBuildType type = PackageBuildType.valueOf(strType.toUpperCase());
            pmd = pmd.set(PackageMetadata.BUILD_TYPE, type);
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

        ImmutableSet<String> qbtEnvToRemove = ImmutableSet.copyOf(options.get(Options.removeQbtEnv));
        if(!qbtEnvToRemove.isEmpty()) {
            pmd = pmd.transform(PackageMetadata.QBT_ENV, (envMap) -> ImmutableMap.copyOf(Maps.filterEntries(envMap, (input) -> !qbtEnvToRemove.contains(input.getKey()))));
        }

        // update normal deps
        DepActions<Pair<NormalDependencyType, PackageTip>> normalActions = buildDepActions(options, Options.addNormalDeps, Options.removeNormalDeps, (input) -> {
            NormalDependencyType ndt = NormalDependencyType.fromTag(input.getLeft());
            PackageTip dt = PackageTip.TYPE.parseRequire(input.getRight());
            return Pair.of(ndt, dt);
        });

        PackageNormalDeps.Builder pnd = pm.get(PackageManifest.NORMAL_DEPS).builder();
        // remove first, so --remove WEAK X --add STRONG X works
        for(Pair<NormalDependencyType, PackageTip> dep : normalActions.removes) {
            Pair<NormalDependencyType, String> val = pnd.map.get(dep.getRight().name);
            if(val == null) {
                throw new IllegalArgumentException("Cannot remove normal dependency " + dep + " because it doesn't exist");
            }
            Pair<NormalDependencyType, PackageTip> exists = Pair.of(val.getLeft(), PackageTip.TYPE.of(dep.getRight().name, val.getRight()));
            if(!dep.equals(exists)) {
                throw new IllegalArgumentException("Cannot remove normal dependency " + dep + " because it doesn't exist (conflicting dependency " + exists + " does exist)");
            }
            pnd = pnd.without(dep.getRight().name);
        }
        for(Pair<NormalDependencyType, PackageTip> dep : normalActions.adds) {
            Pair<NormalDependencyType, String> val = pnd.map.get(dep.getRight().name);
            if(val != null) {
                Pair<NormalDependencyType, PackageTip> exists = Pair.of(val.getLeft(), PackageTip.TYPE.of(dep.getRight().name, val.getRight()));
                if(dep.equals(exists)) {
                    throw new IllegalArgumentException("Cannot add normal dependency " + dep + " because it already exists");
                }
                throw new IllegalArgumentException("Cannot add normal dependency " + dep + " because it conflicts with existing " + exists);
            }
            PackageTip dt = dep.getRight();
            pnd = pnd.with(dt.name, Pair.of(dep.getLeft(), dt.tip));
        }

        // update verify deps
        DepActions<Pair<PackageTip, String>> verifyActions = buildDepActions(options, Options.addVerifyDeps, Options.removeVerifyDeps, (input) -> {
            String type = input.getLeft();
            PackageTip dt = PackageTip.TYPE.parseRequire(input.getRight());
            return Pair.of(dt, type);
        });

        PackageVerifyDeps.Builder pvd = pm.get(PackageManifest.VERIFY_DEPS).builder();

        for(Pair<PackageTip, String> dep : verifyActions.removes) {
            if(!pvd.map.containsKey(dep)) {
                throw new IllegalArgumentException("Cannot remove verify dependency, already doesn't exist: " + dep);
            }
            pvd = pvd.without(dep);
        }

        for(Pair<PackageTip, String> dep : verifyActions.adds) {
            if(pvd.map.containsKey(dep)) {
                throw new IllegalArgumentException("Cannot add verify dependency, already exists: " + dep);
            }
            pvd = pvd.with(dep, ObjectUtils.NULL);
        }

        PackageManifest.Builder pmb = pm.builder().set(PackageManifest.METADATA, pmd).set(PackageManifest.NORMAL_DEPS, pnd).set(PackageManifest.VERIFY_DEPS, pvd);

        RepoManifest.Builder repoManifest = manifest.repos.get(rt).builder();
        repoManifest = repoManifest.transform(RepoManifest.PACKAGES, (pkgs) -> pkgs.with(pt.name, pmb));

        manifest = manifest.builder().with(rt, repoManifest).build();
        manifestResult.deparse(manifest);
        LOGGER.info("Package " + pt + " successfully updated and manifest written successfully");
        return 0;
    }

    private static class DepActions<K> {
        public final ImmutableSet<K> adds;
        public final ImmutableSet<K> removes;
        public DepActions(ImmutableSet<K> adds, ImmutableSet<K> removes) {
            this.adds = adds;
            this.removes = removes;
        }
    }

    private static <M, K> DepActions<K> buildDepActions(OptionsResults<? extends Options> options, OptionsFragment<Options, ImmutableList<M>> addOption, OptionsFragment<Options, ImmutableList<M>> removeOption, Function<M, K> parse) {
        ImmutableMap.Builder<K, Boolean> b = ImmutableMap.builder();
        for(M arg : options.get(addOption)) {
            b.put(parse.apply(arg), true);
        }
        for(M arg : options.get(removeOption)) {
            b.put(parse.apply(arg), false);
        }
        ImmutableSet.Builder<K> adds = ImmutableSet.builder();
        ImmutableSet.Builder<K> removes = ImmutableSet.builder();
        for(Map.Entry<K, Boolean> e : b.build().entrySet()) {
            (e.getValue() ? adds : removes).add(e.getKey());
        }
        return new DepActions<K>(adds.build(), removes.build());
    }
}
