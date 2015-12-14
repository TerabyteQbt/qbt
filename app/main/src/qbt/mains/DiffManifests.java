package qbt.mains;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import java.io.IOException;
import misc1.commons.Maybe;
import misc1.commons.options.NamedBooleanFlagOptionsFragment;
import misc1.commons.options.NamedStringSingletonArgumentOptionsFragment;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import org.apache.commons.lang3.tuple.Pair;
import qbt.HelpTier;
import qbt.NormalDependencyType;
import qbt.PackageManifest;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.QbtManifest;
import qbt.RepoManifest;
import qbt.VcsVersionDigest;
import qbt.config.QbtConfig;
import qbt.diffmanifests.MapDiffer;
import qbt.diffmanifests.NoEditMapDiffer;
import qbt.diffmanifests.Runner;
import qbt.diffmanifests.SetDiffer;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public final class DiffManifests extends QbtCommand<DiffManifests.Options> {
    @QbtCommandName("diffManifests")
    public static interface Options extends QbtCommandOptions {
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final ManifestOptionsDelegate<Options> lhs = new ManifestOptionsDelegate<Options>("lhs");
        public static final ManifestOptionsDelegate<Options> rhs = new ManifestOptionsDelegate<Options>("rhs");
        public static final OptionsFragment<Options, ?, Boolean> noPrefix = new NamedBooleanFlagOptionsFragment<Options>(ImmutableList.of("--no-prefix"), "Don't prefix each line of output with repo banner (the default is to prefix)");
        public static final OptionsFragment<Options, ?, String> onPackageAdd = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onPackageAdd"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onPackageDel = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onPackageDel"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onPackageMetadataAdd = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onPackageMetadataAdd"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onPackageMetadataDel = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onPackageMetadataDel"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onNormalDepAdd = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onNormalDepAdd"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onNormalDepDel = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onNormalDepDel"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onReplaceDepAdd = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onReplaceDepAdd"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onReplaceDepDel = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onReplaceDepDel"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onVerifyDepAdd = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onVerifyDepAdd"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onVerifyDepDel = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onVerifyDepDel"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onRepoAdd = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onRepoAdd"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onRepoDel = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onRepoDel"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onRepoEdit = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onRepoEdit"), Maybe.<String>of(null), "Command");
        public static final OptionsFragment<Options, ?, String> onRepoEditPlus = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--onRepoEditPlus"), Maybe.<String>of(null), "Command");
    }

    @Override
    public Class<Options> getOptionsClass() {
        return Options.class;
    }

    @Override
    public HelpTier getHelpTier() {
        return HelpTier.ARCANE;
    }

    @Override
    public String getDescription() {
        return "compare two qbt-manifest files and run a command on each difference";
    }

    @Override
    public boolean isProgrammaticOutput() {
        return true;
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        final QbtConfig config = Options.config.getConfig(options);

        QbtManifest lhs = Options.lhs.getResult(options).parse();
        QbtManifest rhs = Options.rhs.getResult(options).parse();

        diffManifest(options, config, lhs, rhs);

        return 0;
    }

    private Runner repoRunner(OptionsResults<? extends Options> options, String command, RepoTip repo) {
        if(command == null) {
            return Runner.dead();
        }
        Runner r = Runner.real(options.get(Options.noPrefix) ? null: repo.toString(), command);
        r = r.addEnv("REPO_NAME", repo.name);
        r = r.addEnv("REPO_TIP", repo.tip);
        return r;
    }

    private Runner packageRunner(OptionsResults<? extends Options> options, String command, RepoTip repo, String pkg) {
        if(command == null) {
            return Runner.dead();
        }
        Runner r = Runner.real(options.get(Options.noPrefix) ? null: repo.toPackage(pkg).toString(), command);
        r = r.addEnv("REPO_NAME", repo.name);
        r = r.addEnv("REPO_TIP", repo.tip);
        r = r.addEnv("PACKAGE_NAME", pkg);
        return r;
    }

    private Runner runnerAddRepo(Runner r, String suffix, VcsVersionDigest version, PinnedRepoAccessor result) {
        r = r.findCommit(result);
        r = r.addEnv("REPO_VERSION" + suffix, version.getRawDigest().toString());
        r = r.addEnv("REPO_TREE" + suffix, result.getSubtree("").getRawDigest().toString());
        return r;
    }

    private void diffManifest(final OptionsResults<? extends Options> options, final QbtConfig config, QbtManifest lhs, QbtManifest rhs) {
        new MapDiffer<RepoTip, RepoManifest>(lhs.repos, rhs.repos, RepoTip.TYPE.COMPARATOR) {
            @Override
            protected void add(RepoTip repo, RepoManifest rhs) {
                PinnedRepoAccessor rhsResult = config.localPinsRepo.requirePin(repo, rhs.version);

                {
                    Runner r = repoRunner(options, options.get(Options.onRepoAdd), repo);
                    r = runnerAddRepo(r, "", rhs.version, rhsResult);
                    r = r.checkout(rhs.version);
                    r.run();
                }

                {
                    Runner r = repoRunner(options, options.get(Options.onRepoEditPlus), repo);
                    r = r.findCommit(rhsResult);
                    r = r.addEnv("REPO_TREE_LHS", rhsResult.getLocalVcs().emptyTree().getRawDigest().toString());
                    r = r.addEnv("REPO_TREE_RHS", rhsResult.getSubtree("").getRawDigest().toString());
                    r = r.checkout(rhs.version);
                    r.run();
                }

                diffRepo(options, config, repo, RepoManifest.builder(rhs.version).build(), rhs);
            }

            @Override
            protected void del(RepoTip repo, RepoManifest lhs) {
                diffRepo(options, config, repo, lhs, RepoManifest.builder(lhs.version).build());

                PinnedRepoAccessor lhsResult = config.localPinsRepo.requirePin(repo, lhs.version);

                {
                    Runner r = repoRunner(options, options.get(Options.onRepoEditPlus), repo);
                    r = r.findCommit(lhsResult);
                    r = r.addEnv("REPO_TREE_LHS", lhsResult.getSubtree("").getRawDigest().toString());
                    r = r.addEnv("REPO_TREE_RHS", lhsResult.getLocalVcs().emptyTree().getRawDigest().toString());
                    r = r.checkout(lhs.version);
                    r.run();
                }

                {
                    Runner r = repoRunner(options, options.get(Options.onRepoDel), repo);
                    r = runnerAddRepo(r, "", lhs.version, lhsResult);
                    r = r.checkout(lhs.version);
                    r.run();
                }
            }

            @Override
            protected void edit(RepoTip repo, RepoManifest lhs, RepoManifest rhs) {
                diffRepo(options, config, repo, lhs, rhs);
            }
        }.diff();
    }

    private void diffRepo(final OptionsResults<? extends Options> options, final QbtConfig config, final RepoTip repo, RepoManifest lhs, RepoManifest rhs) {
        if(!lhs.version.equals(rhs.version)) {
            PinnedRepoAccessor lhsResult = config.localPinsRepo.requirePin(repo, lhs.version);
            PinnedRepoAccessor rhsResult = config.localPinsRepo.requirePin(repo, rhs.version);

            {
                Runner r = repoRunner(options, options.get(Options.onRepoEdit), repo);
                r = runnerAddRepo(r, "_LHS", lhs.version, lhsResult);
                r = runnerAddRepo(r, "_RHS", rhs.version, rhsResult);
                r = r.checkout(rhs.version);
                r.run();
            }

            {
                Runner r = repoRunner(options, options.get(Options.onRepoEditPlus), repo);
                r = r.findCommit(lhsResult);
                r = r.findCommit(rhsResult);
                r = r.addEnv("REPO_TREE_LHS", lhsResult.getSubtree("").getRawDigest().toString());
                r = r.addEnv("REPO_TREE_RHS", rhsResult.getSubtree("").getRawDigest().toString());
                r = r.checkout(rhs.version);
                r.run();
            }
        }

        new MapDiffer<String, PackageManifest>(lhs.packages, rhs.packages, Ordering.<String>natural()) {
            @Override
            protected void add(String pkg, PackageManifest rhs) {
                packageRunner(options, options.get(Options.onPackageAdd), repo, pkg).run();
                diffPackage(options, config, repo, pkg, PackageManifest.emptyBuilder().build(), rhs);
            }

            @Override
            protected void del(String pkg, PackageManifest lhs) {
                diffPackage(options, config, repo, pkg, lhs, PackageManifest.emptyBuilder().build());
                packageRunner(options, options.get(Options.onPackageDel), repo, pkg).run();
            }

            @Override
            protected void edit(String pkg, PackageManifest lhs, PackageManifest rhs) {
                diffPackage(options, config, repo, pkg, lhs, rhs);
            }
        }.diff();
    }

    private void diffPackage(final OptionsResults<? extends Options> options, QbtConfig config, final RepoTip repo, final String pkg, PackageManifest lhs, PackageManifest rhs) {
        new NoEditMapDiffer<String, String>(lhs.metadata.toStringMap(), rhs.metadata.toStringMap(), Ordering.<String>natural()) {
            @Override
            protected void add(String key, String value) {
                Runner r = packageRunner(options, options.get(Options.onPackageMetadataAdd), repo, pkg);
                r = r.addEnv("PACKAGE_METADATA_ITEM", key);
                r = r.addEnv("PACKAGE_METADATA_VALUE", value);
                r.run();
            }

            @Override
            protected void del(String key, String value) {
                Runner r = packageRunner(options, options.get(Options.onPackageMetadataDel), repo, pkg);
                r = r.addEnv("PACKAGE_METADATA_ITEM", key);
                r = r.addEnv("PACKAGE_METADATA_VALUE", value);
                r.run();
            }
        }.diff();

        new NoEditMapDiffer<String, Pair<NormalDependencyType, String>>(lhs.normalDeps, rhs.normalDeps, Ordering.<String>natural()) {
            @Override
            protected void add(String depPackage, Pair<NormalDependencyType, String> pair) {
                Runner r = packageRunner(options, options.get(Options.onNormalDepAdd), repo, pkg);
                r = r.addEnv("DEPENDENCY_NAME", depPackage);
                r = r.addEnv("DEPENDENCY_TIP", pair.getRight());
                r = r.addEnv("DEPENDENCY_TYPE", pair.getLeft().getTag());
                r.run();
            }

            @Override
            protected void del(String depPackage, Pair<NormalDependencyType, String> pair) {
                Runner r = packageRunner(options, options.get(Options.onNormalDepDel), repo, pkg);
                r = r.addEnv("DEPENDENCY_NAME", depPackage);
                r = r.addEnv("DEPENDENCY_TIP", pair.getRight());
                r = r.addEnv("DEPENDENCY_TYPE", pair.getLeft().getTag());
                r.run();
            }
        }.diff();

        new NoEditMapDiffer<PackageTip, String>(lhs.replaceDeps, rhs.replaceDeps, PackageTip.TYPE.COMPARATOR) {
            @Override
            protected void add(PackageTip dep, String toTip) {
                Runner r = packageRunner(options, options.get(Options.onReplaceDepAdd), repo, pkg);
                r = r.addEnv("DEPENDENCY_NAME", dep.name);
                r = r.addEnv("DEPENDENCY_FROM_TIP", dep.tip);
                r = r.addEnv("DEPENDENCY_TO_TIP", toTip);
                r.run();
            }

            @Override
            protected void del(PackageTip dep, String toTip) {
                Runner r = packageRunner(options, options.get(Options.onReplaceDepDel), repo, pkg);
                r = r.addEnv("DEPENDENCY_NAME", dep.name);
                r = r.addEnv("DEPENDENCY_FROM_TIP", dep.tip);
                r = r.addEnv("DEPENDENCY_TO_TIP", toTip);
                r.run();
            }
        }.diff();

        new SetDiffer<Pair<PackageTip, String>>(lhs.verifyDeps, rhs.verifyDeps, QbtManifest.verifyDepComparator) {
            @Override
            protected void add(Pair<PackageTip, String> dep) {
                Runner r = packageRunner(options, options.get(Options.onVerifyDepAdd), repo, pkg);
                r = r.addEnv("DEPENDENCY_NAME", dep.getLeft().name);
                r = r.addEnv("DEPENDENCY_TIP", dep.getLeft().tip);
                r = r.addEnv("DEPENDENCY_TYPE", dep.getRight());
                r.run();
            }

            @Override
            protected void del(Pair<PackageTip, String> dep) {
                Runner r = packageRunner(options, options.get(Options.onVerifyDepDel), repo, pkg);
                r = r.addEnv("DEPENDENCY_NAME", dep.getLeft().name);
                r = r.addEnv("DEPENDENCY_TIP", dep.getLeft().tip);
                r = r.addEnv("DEPENDENCY_TYPE", dep.getRight());
                r.run();
            }
        }.diff();
    }
}
