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

public class RepoActionOptionsDelegate<O> implements OptionsDelegate<O> {
    private final OptionsLibrary<O> o = OptionsLibrary.of();
    public final OptionsFragment<O, ImmutableList<String>> repos = o.oneArg("repo").helpDesc("Act on this repo");
    public final OptionsFragment<O, ImmutableList<String>> packages = o.oneArg("package").helpDesc("Act on the repo containing this package");
    public final OptionsFragment<O, Boolean> overrides = o.zeroArg("overrides").transform(o.flag()).helpDesc("Act on all overridden repos");
    public final OptionsFragment<O, Boolean> all = o.zeroArg("all").transform(o.flag()).helpDesc("Act on all repos");
    public final OptionsFragment<O, ImmutableList<String>> groovyRepos = o.oneArg("groovyRepos").helpDesc("Evaluate this groovy to choose repos");

    private final NoArgsBehaviour noArgsBehaviour;

    public RepoActionOptionsDelegate(NoArgsBehaviour noArgsBehaviour) {
        this.noArgsBehaviour = noArgsBehaviour;
    }

    public interface NoArgsBehaviour {
        public void run(ImmutableSet.Builder<RepoTip> b, QbtConfig config, QbtManifest manifest);

        public static final NoArgsBehaviour EMPTY = (b, config, manifest) -> {
        };
        public static final NoArgsBehaviour OVERRIDES = RepoActionOptionsDelegate::addOverrides;
        public static final NoArgsBehaviour THROW = (b, config, manifest) -> {
            throw new OptionsException("Some form of repo selection is required.");
        };
        public static final NoArgsBehaviour ALL = (b, config, manifest) -> b.addAll(manifest.repos.keySet());
    }

    public Collection<RepoTip> getRepos(QbtConfig config, QbtManifest manifest, OptionsResults<? extends O> options) {
        boolean hadNoArgs = true;

        ImmutableSet.Builder<RepoTip> reposBuilder = ImmutableSet.builder();
        for(String arg : options.get(repos)) {
            hadNoArgs = false;
            reposBuilder.add(RepoTip.TYPE.parseRequire(arg));
        }
        for(String arg : options.get(packages)) {
            hadNoArgs = false;
            PackageTip pkg = PackageTip.TYPE.parseRequire(arg);
            RepoTip repo = manifest.packageToRepo.get(pkg);
            if(repo == null) {
                throw new IllegalArgumentException("No such package [tip]: " + pkg);
            }
            reposBuilder.add(repo);
        }
        if(options.get(overrides)) {
            hadNoArgs = false;
            addOverrides(reposBuilder, config, manifest);
        }
        if(options.get(all)) {
            hadNoArgs = false;
            reposBuilder.addAll(manifest.repos.keySet());
        }
        for(String groovy : options.get(groovyRepos)) {
            hadNoArgs = false;
            reposBuilder.addAll(PackageRepoSelection.evalRepos(config, manifest, groovy));
        }
        if(hadNoArgs) {
            noArgsBehaviour.run(reposBuilder, config, manifest);
        }
        return reposBuilder.build();
    }

    private static void addOverrides(ImmutableSet.Builder<RepoTip> reposBuilder, QbtConfig config, QbtManifest manifest) {
        for(Map.Entry<RepoTip, RepoManifest> e : manifest.repos.entrySet()) {
            if(config.localRepoFinder.findLocalRepo(e.getKey()) != null) {
                reposBuilder.add(e.getKey());
            }
        }
    }
}
