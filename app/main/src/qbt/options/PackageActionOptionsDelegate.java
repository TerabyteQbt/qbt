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
import qbt.config.QbtConfig;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public class PackageActionOptionsDelegate<O> implements OptionsDelegate<O> {
    public final OptionsFragment<O, ?, ImmutableList<String>> packages = new NamedStringListArgumentOptionsFragment<O>(ImmutableList.of("--package"), "Act on this package");
    public final OptionsFragment<O, ?, ImmutableList<String>> repos = new NamedStringListArgumentOptionsFragment<O>(ImmutableList.of("--repo"), "Act on all packages in this repo");
    public final OptionsFragment<O, ?, Boolean> outward = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--outward"), "Act on all packages outward of [otherwise] specified packages");
    public final OptionsFragment<O, ?, Boolean> overrides = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--overrides"), "Act on all overridden packages");
    public final OptionsFragment<O, ?, Boolean> all = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--all"), "Act on all packages");
    public final OptionsFragment<O, ?, ImmutableList<String>> groovyPackages = new NamedStringListArgumentOptionsFragment<O>(ImmutableList.of("--groovyPackages"), "Evaluate this groovy to choose packages");

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
