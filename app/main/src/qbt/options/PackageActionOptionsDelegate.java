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
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.PackageManifest;
import qbt.QbtManifest;
import qbt.RepoManifest;
import qbt.config.QbtConfig;
import qbt.map.DependencyComputer;
import qbt.map.SimpleDependencyComputer;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public class PackageActionOptionsDelegate<O> implements OptionsDelegate<O> {
    public final OptionsFragment<O, ?, ImmutableList<String>> packages = new NamedStringListArgumentOptionsFragment<O>(ImmutableList.of("--package"), "Act on this package");
    public final OptionsFragment<O, ?, ImmutableList<String>> repos = new NamedStringListArgumentOptionsFragment<O>(ImmutableList.of("--repo"), "Act on all packages in this repo");
    public final OptionsFragment<O, ?, Boolean> outward = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--outward"), "Act on all packages outward of [otherwise] specified packages");
    public final OptionsFragment<O, ?, Boolean> overrides = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--overrides"), "Act on all overridden packages");
    public final OptionsFragment<O, ?, Boolean> all = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--all"), "Act on all packages");

    private final NoArgsBehaviour noArgsBehaviour;

    public PackageActionOptionsDelegate(NoArgsBehaviour noArgsBehaviour) {
        this.noArgsBehaviour = noArgsBehaviour;
    }

    public enum NoArgsBehaviour {
        EMPTY,
        OVERRIDES,
        THROW;
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
        if(hadNoArgs) {
            switch(noArgsBehaviour) {
                case EMPTY:
                    break;

                case OVERRIDES:
                    addOverrides(packagesBuilder, config, manifest);
                    break;

                case THROW:
                    throw new OptionsException("Some form of package selection is required.");
            }
        }

        if(options.get(outward)) {
            addOutwards(packagesBuilder, manifest, packagesBuilder.build());
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

    private static void addOutwards(ImmutableSet.Builder<PackageTip> packagesBuilder, QbtManifest manifest, final ImmutableSet<PackageTip> from) {
        DependencyComputer<PackageManifest, Boolean> usesOutwardsComputer = new SimpleDependencyComputer<Boolean>(manifest) {
            @Override
            protected Boolean map(PackageManifest packageManifest, PackageTip packageTip, Map<String, Pair<NormalDependencyType, Boolean>> dependencyResults) {
                if(from.contains(packageTip)) {
                    return true;
                }
                for(Pair<NormalDependencyType, Boolean> dependencyResult : dependencyResults.values()) {
                    if(dependencyResult.getRight()) {
                        return true;
                    }
                }
                return false;
            }
        };
        for(PackageTip pkg : manifest.packageToRepo.keySet()) {
            if(usesOutwardsComputer.compute(pkg)) {
                packagesBuilder.add(pkg);
            }
        }
    }
}
