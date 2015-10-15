package qbt.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Map;
import misc1.commons.options.NamedBooleanFlagOptionsFragment;
import misc1.commons.options.NamedStringListArgumentOptionsFragment;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsException;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import qbt.QbtManifest;
import qbt.RepoManifest;
import qbt.config.QbtConfig;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public class RepoActionOptionsDelegate<O> implements OptionsDelegate<O> {
    public final OptionsFragment<O, ?, ImmutableList<String>> repos = new NamedStringListArgumentOptionsFragment<O>(ImmutableList.of("--repo"), "Act on this repo");
    public final OptionsFragment<O, ?, ImmutableList<String>> packages = new NamedStringListArgumentOptionsFragment<O>(ImmutableList.of("--package"), "Act on the repo containing this package");
    public final OptionsFragment<O, ?, Boolean> overrides = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--overrides"), "Act on all overridden repos");
    public final OptionsFragment<O, ?, Boolean> all = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--all"), "Act on all repos");
    public final OptionsFragment<O, ?, ImmutableList<String>> groovyRepos = new NamedStringListArgumentOptionsFragment<O>(ImmutableList.of("--groovyRepos"), "Evaluate this groovy to choose repos");

    private final NoArgsBehaviour noArgsBehaviour;

    public RepoActionOptionsDelegate(NoArgsBehaviour noArgsBehaviour) {
        this.noArgsBehaviour = noArgsBehaviour;
    }

    public enum NoArgsBehaviour {
        EMPTY,
        OVERRIDES,
        THROW;
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
            switch(noArgsBehaviour) {
                case EMPTY:
                    break;

                case OVERRIDES:
                    addOverrides(reposBuilder, config, manifest);
                    break;

                case THROW:
                    throw new OptionsException("Some form of repo selection is required.");
            }
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
