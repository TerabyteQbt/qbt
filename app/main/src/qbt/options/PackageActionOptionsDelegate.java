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
package qbt.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Map;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsException;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;
import qbt.config.QbtConfig;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public class PackageActionOptionsDelegate<O> implements OptionsDelegate<O> {
    private final OptionsLibrary<O> o = OptionsLibrary.of();
    public final OptionsFragment<O, ImmutableList<String>> packages = o.oneArg("package").helpDesc("Act on this package");
    public final OptionsFragment<O, ImmutableList<String>> repos = o.oneArg("repo").helpDesc("Act on all packages in this repo");
    public final OptionsFragment<O, Boolean> outward = o.zeroArg("outward").transform(o.flag()).helpDesc("Act on all packages outward of [otherwise] specified packages");
    public final OptionsFragment<O, Boolean> overrides = o.zeroArg("overrides").transform(o.flag()).helpDesc("Act on all overridden packages");
    public final OptionsFragment<O, Boolean> all = o.zeroArg("all").transform(o.flag()).helpDesc("Act on all packages");
    public final OptionsFragment<O, ImmutableList<String>> groovyPackages = o.oneArg("groovyPackages").helpDesc("Evaluate this groovy to choose packages");

    private final NoArgsBehaviour noArgsBehaviour;

    public PackageActionOptionsDelegate(NoArgsBehaviour noArgsBehaviour) {
        this.noArgsBehaviour = noArgsBehaviour;
    }

    public interface NoArgsBehaviour {
        public void run(ImmutableSet.Builder<PackageTip> b, QbtConfig config, QbtManifest manifest);

        public static final NoArgsBehaviour EMPTY = (b, config, manifest) -> {
        };
        public static final NoArgsBehaviour OVERRIDES = PackageActionOptionsDelegate::addOverrides;
        public static final NoArgsBehaviour THROW = (b, config, manifest) -> {
            throw new OptionsException("Some form of package selection is required.");
        };
    }

    public Collection<PackageTip> getPackages(QbtConfig config, QbtManifest manifest, OptionsResults<? extends O> options) {
        boolean hadNoArgs = true;

        ImmutableSet.Builder<PackageTip> packagesBuilder = ImmutableSet.builder();
        for(String arg : options.get(packages)) {
            hadNoArgs = false;
            packagesBuilder.add(PackageTip.TYPE.parseRequire(arg));
        }
        for(String arg : options.get(repos)) {
            hadNoArgs = false;
            RepoTip repo = RepoTip.TYPE.parseRequire(arg);
            RepoManifest repoManifest = manifest.repos.get(repo);
            if(repoManifest == null) {
                throw new IllegalArgumentException("No such repo [tip]: " + repo);
            }
            for(String packageName : repoManifest.packages.keySet()) {
                packagesBuilder.add(repo.toPackage(packageName));
            }
        }
        if(options.get(overrides)) {
            hadNoArgs = false;
            addOverrides(packagesBuilder, config, manifest);
        }
        if(options.get(all)) {
            hadNoArgs = false;
            packagesBuilder.addAll(manifest.packageToRepo.keySet());
        }
        for(String groovy : options.get(groovyPackages)) {
            hadNoArgs = false;
            packagesBuilder.addAll(PackageRepoSelection.evalPackages(config, manifest, groovy));
        }
        if(hadNoArgs) {
            noArgsBehaviour.run(packagesBuilder, config, manifest);
        }

        if(options.get(outward)) {
            packagesBuilder.addAll(PackageRepoSelection.outwardsClosure(manifest, packagesBuilder.build()));
        }

        return packagesBuilder.build();
    }

    private static void addOverrides(ImmutableSet.Builder<PackageTip> packagesBuilder, QbtConfig config, QbtManifest manifest) {
        for(Map.Entry<RepoTip, RepoManifest> e : manifest.repos.entrySet()) {
            if(config.localRepoFinder.findLocalRepo(e.getKey()) != null) {
                for(String pkg : e.getValue().packages.keySet()) {
                    packagesBuilder.add(e.getKey().toPackage(pkg));
                }
            }
        }
    }
}
