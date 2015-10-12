package qbt.mains;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import misc1.commons.Maybe;
import misc1.commons.options.NamedEnumSingletonArgumentOptionsFragment;
import misc1.commons.options.NamedStringSingletonArgumentOptionsFragment;
import misc1.commons.options.OptionsException;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.NormalDependencyType;
import qbt.PackageManifest;
import qbt.PackageTip;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.QbtManifest;
import qbt.QbtTempDir;
import qbt.RepoManifest;
import qbt.VcsVersionDigest;
import qbt.config.QbtConfig;
import qbt.metadata.PackageMetadataType;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.ShellActionOptionsDelegate;
import qbt.options.ShellActionOptionsResult;
import qbt.repo.PinnedRepoAccessor;
import qbt.utils.ProcessHelper;
import qbt.vcs.LocalVcs;
import qbt.vcs.Repository;

public final class MergeManifests extends QbtCommand<MergeManifests.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergeManifests.class);

    @QbtCommandName("mergeManifests")
    public static interface Options extends QbtCommandOptions {
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final OptionsFragment<Options, ?, String> lhsName = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--lhsName"), Maybe.of("LHS"), "Name of \"left\" side (for conflict markers)");
        public static final OptionsFragment<Options, ?, String> mhsName = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--mhsName"), Maybe.of("MHS"), "Name of \"middle\" side (for conflict markers)");
        public static final OptionsFragment<Options, ?, String> rhsName = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--rhsName"), Maybe.of("RHS"), "Name of \"right\" side (for conflict markers)");
        public static final ManifestOptionsDelegate<Options> lhs = new ManifestOptionsDelegate<Options>("lhs");
        public static final ManifestOptionsDelegate<Options> mhs = new ManifestOptionsDelegate<Options>("mhs");
        public static final ManifestOptionsDelegate<Options> rhs = new ManifestOptionsDelegate<Options>("rhs");
        public static final ManifestOptionsDelegate<Options> out = new ManifestOptionsDelegate<Options>("out");
        public static final OptionsFragment<Options, ?, StrategyEnum> strategy = new NamedEnumSingletonArgumentOptionsFragment<Options, StrategyEnum>(StrategyEnum.class, ImmutableList.of("--strategy"), Maybe.<StrategyEnum>of(null), "\"Strategy\" for satellites");
        public static final ShellActionOptionsDelegate<Options> shellAction = new ShellActionOptionsDelegate<Options>();
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
        return "perform a three way merge of qbt-manifest files, performing merges in individual repositories as needed";
    }

    public interface Strategy {
        void invoke(PackageTip repo, Repository repository, VcsVersionDigest lhs, VcsVersionDigest mhs, VcsVersionDigest rhs);
    }

    public static enum StrategyEnum implements Strategy {
        merge {
            @Override
            public void invoke(PackageTip repo, Repository repository, VcsVersionDigest lhs, VcsVersionDigest mhs, VcsVersionDigest rhs) {
                if(!repository.isAncestorOf(mhs, lhs)) {
                    throw new RuntimeException("Actual merge of " + lhs.getRawDigest() + " (and " + rhs.getRawDigest() + ") over " + mhs.getRawDigest() + " is not fast-forward!");
                }
                if(!repository.isAncestorOf(mhs, rhs)) {
                    throw new RuntimeException("Actual merge of (" + lhs.getRawDigest() + " and) " + rhs.getRawDigest() + " over " + mhs.getRawDigest() + " is not fast-forward!");
                }
                repository.checkout(lhs);
                repository.merge(rhs);
            }
        },
        rebase {
            @Override
            public void invoke(PackageTip repo, Repository repository, VcsVersionDigest lhs, VcsVersionDigest mhs, VcsVersionDigest rhs) {
                repository.checkout(lhs);
                repository.rebase(mhs, rhs);
            }
        };
    }

    private static final class Context {
        public final QbtConfig config;
        public final Strategy strategy;

        public Context(QbtConfig config, final Strategy strategy) {
            this.config = config;
            this.strategy = strategy;
        }
    }

    private static Strategy parseStrategyOptions(OptionsResults<? extends Options> options) {
        ImmutableList.Builder<Strategy> b = ImmutableList.builder();
        final ShellActionOptionsResult shellActionOptionsResult = Options.shellAction.getResultsOptional(options);
        if(shellActionOptionsResult != null) {
            b.add(new Strategy() {
                @Override
                public void invoke(final PackageTip repo, Repository repository, VcsVersionDigest lhs, VcsVersionDigest mhs, VcsVersionDigest rhs) {
                    repository.checkout(lhs);
                    ProcessHelper p = new ProcessHelper(repository.getRoot(), shellActionOptionsResult.commandArray);
                    p = p.putEnv("LHS", lhs.getRawDigest().toString());
                    p = p.putEnv("MHS", mhs.getRawDigest().toString());
                    p = p.putEnv("RHS", rhs.getRawDigest().toString());
                    if(shellActionOptionsResult.isInteractive) {
                        LOGGER.info("Invoking custom merge for merge " + repo + "...");
                        LOGGER.info("    LHS is " + lhs.getRawDigest() + " (in $LHS and checked out)");
                        LOGGER.info("    MHS is " + mhs.getRawDigest() + " (in $MHS)");
                        LOGGER.info("    RHS is " + rhs.getRawDigest() + " (in $RHS)");
                        LOGGER.info("    Exit with success to indicate HEAD is result");
                        LOGGER.info("    Exit with failure to fail mergeManifests entirely");
                        if(!repository.isAncestorOf(mhs, lhs)) {
                            LOGGER.warn("Careful, LHS (" + lhs.getRawDigest() + ") is not a descendent of MHS (" + mhs.getRawDigest() + ")");
                        }
                        if(!repository.isAncestorOf(mhs, rhs)) {
                            LOGGER.warn("Careful, RHS (" + rhs.getRawDigest() + ") is not a descendent of MHS (" + mhs.getRawDigest() + ")");
                        }
                        p = p.inheritInput();
                        p = p.inheritOutput();
                        p = p.inheritError();
                        p.completeVoid();
                    }
                    else {
                        p = p.combineError();
                        p.completeLinesCallback(new Function<String, Void>() {
                            @Override
                            public Void apply(String line) {
                                System.out.println("[" + repo + "] " + line);
                                return null;
                            }
                        });
                    }
                }
            });
        }
        StrategyEnum strategy = options.get(Options.strategy);
        if(strategy != null) {
            b.add(strategy);
        }
        ImmutableList<Strategy> rets = b.build();
        if(rets.isEmpty()) {
            return StrategyEnum.merge;
        }
        if(rets.size() > 1) {
            throw new OptionsException("Multiple strategies specified?");
        }
        return rets.get(0);
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        QbtConfig config = Options.config.getConfig(options);

        Context context = new Context(config, parseStrategyOptions(options));

        QbtManifest lhs = Options.lhs.getResult(options).parse();
        QbtManifest mhs = Options.mhs.getResult(options).parse();
        QbtManifest rhs = Options.rhs.getResult(options).parse();
        String lhsName = options.get(Options.lhsName);
        String mhsName = options.get(Options.mhsName);
        String rhsName = options.get(Options.rhsName);

        ImmutableSet.Builder<PackageTip> repos = ImmutableSet.builder();
        repos.addAll(lhs.repos.keySet());
        repos.addAll(mhs.repos.keySet());
        repos.addAll(rhs.repos.keySet());

        Triple<QbtManifest, QbtManifest, QbtManifest> merged = qbtManifestMerger.merge(context, "", ObjectUtils.NULL, lhs, mhs, rhs);
        boolean conflicted = Options.out.getResult(options).deparseConflict(lhsName, merged.getLeft(), mhsName, merged.getMiddle(), rhsName, merged.getRight());

        return conflicted ? 1 : 0;
    }

    private static abstract class Merger<K, V> {
        public final Triple<V, V, V> merge(Context context, String label, K k, V lhs, V mhs, V rhs) {
            if(Objects.equal(lhs, mhs)) {
                return Triple.of(rhs, rhs, rhs);
            }
            if(Objects.equal(rhs, mhs)) {
                return Triple.of(lhs, lhs, lhs);
            }
            if(Objects.equal(lhs, rhs)) {
                return Triple.of(lhs, lhs, lhs);
            }
            if(lhs == null || rhs == null) {
                LOGGER.error("[" + label + "] DELETE/EDIT conflict");
                return Triple.of(lhs, mhs, rhs);
            }
            if(mhs == null) {
                LOGGER.error("[" + label + "] ADD/ADD conflict");
                return Triple.of(lhs, mhs, rhs);
            }
            return mergeConflict(context, label, k, lhs, mhs, rhs);
        }

        protected String combineLabel(String label, Object e) {
            if(!label.isEmpty()) {
                label += "/";
            }
            return label + e;
        }

        protected abstract Triple<V, V, V> mergeConflict(Context context, String label, K k, V lhs, V mhs, V rhs);
    }

    private static class MapMerger<K1, K2, V> extends Merger<K1, Map<K2, V>> {
        private final Comparator<K2> comparator;
        private final Merger<Pair<K1, K2>, V> entryMerger;

        public MapMerger(Comparator<K2> comparator, Merger<Pair<K1, K2>, V> entryMerger) {
            this.comparator = comparator;
            this.entryMerger = entryMerger;
        }

        @Override
        protected Triple<Map<K2, V>, Map<K2, V>, Map<K2, V>> mergeConflict(Context context, String label, K1 k1, Map<K2, V> lhs, Map<K2, V> mhs, Map<K2, V> rhs) {
            Set<K2> k2s = Sets.newTreeSet(comparator);
            k2s.addAll(lhs.keySet());
            k2s.addAll(mhs.keySet());
            k2s.addAll(rhs.keySet());
            ImmutableMap.Builder<K2, V> lhsBuilder = ImmutableMap.builder();
            ImmutableMap.Builder<K2, V> mhsBuilder = ImmutableMap.builder();
            ImmutableMap.Builder<K2, V> rhsBuilder = ImmutableMap.builder();
            for(K2 k2 : k2s) {
                Triple<V, V, V> result = entryMerger.merge(context, combineLabel(label, k2), Pair.of(k1, k2), lhs.get(k2), mhs.get(k2), rhs.get(k2));
                if(result.getLeft() != null) {
                    lhsBuilder.put(k2, result.getLeft());
                }
                if(result.getMiddle() != null) {
                    mhsBuilder.put(k2, result.getMiddle());
                }
                if(result.getRight() != null) {
                    rhsBuilder.put(k2, result.getRight());
                }
            }
            return Triple.<Map<K2, V>, Map<K2, V>, Map<K2, V>>of(lhsBuilder.build(), mhsBuilder.build(), rhsBuilder.build());
        }
    }

    private static class TrivialMerger<K, V> extends Merger<K, V> {
        @Override
        protected Triple<V, V, V> mergeConflict(Context context, String label, K k, V lhs, V mhs, V rhs) {
            LOGGER.error("[" + label + "] EDIT/EDIT conflict");
            return Triple.of(lhs, mhs, rhs);
        }
    }

    private static final Merger<PackageTip, VcsVersionDigest> versionMerger = new Merger<PackageTip, VcsVersionDigest>() {
        @Override
        protected Triple<VcsVersionDigest, VcsVersionDigest, VcsVersionDigest> mergeConflict(Context context, String label, PackageTip repo, VcsVersionDigest lhs, VcsVersionDigest mhs, VcsVersionDigest rhs) {
            PinnedRepoAccessor lhsResult = context.config.localPinsRepo.requirePin(repo, lhs);
            LocalVcs lhsLocalVcs = lhsResult.getLocalVcs();

            PinnedRepoAccessor mhsResult = context.config.localPinsRepo.requirePin(repo, mhs);
            LocalVcs mhsLocalVcs = mhsResult.getLocalVcs();

            PinnedRepoAccessor rhsResult = context.config.localPinsRepo.requirePin(repo, rhs);
            LocalVcs rhsLocalVcs = rhsResult.getLocalVcs();

            if(!lhsLocalVcs.equals(mhsLocalVcs) || !rhsLocalVcs.equals(mhsLocalVcs)) {
                LOGGER.error("[" + label + "] Found mis-matched VCS: " + lhsLocalVcs + ", " + mhsLocalVcs + ", " + rhsLocalVcs);
                return Triple.of(lhs, mhs, rhs);
            }
            LocalVcs localVcs = lhsLocalVcs;

            try(QbtTempDir tempDir = new QbtTempDir()) {
                Path repoDir = tempDir.path;
                localVcs.createWorkingRepo(repoDir);
                lhsResult.findCommit(repoDir);
                mhsResult.findCommit(repoDir);
                rhsResult.findCommit(repoDir);
                Repository repository = localVcs.getRepository(repoDir);
                context.strategy.invoke(repo, repository, lhs, mhs, rhs);
                VcsVersionDigest result = repository.getCurrentCommit();
                lhsResult.addPin(repoDir, result);
                rhsResult.addPin(repoDir, result);

                return Triple.of(result, result, result);
            }
            catch(RuntimeException e) {
                LOGGER.error("[" + label + "]", e);
                return Triple.of(lhs, mhs, rhs);
            }
        }
    };

    private static final Merger<ObjectUtils.Null, Map<String, String>> metadataMerger = new MapMerger<ObjectUtils.Null, String, String>(Ordering.<String>natural(), new TrivialMerger<Pair<ObjectUtils.Null, String>, String>());

    private static final Merger<ObjectUtils.Null, Map<String, Pair<NormalDependencyType, String>>> normalDepsMerger = new MapMerger<ObjectUtils.Null, String, Pair<NormalDependencyType, String>>(Ordering.<String>natural(), new TrivialMerger<Pair<ObjectUtils.Null, String>, Pair<NormalDependencyType, String>>());

    private static final Merger<ObjectUtils.Null, Map<PackageTip, String>> replaceDepsMerger = new MapMerger<ObjectUtils.Null, PackageTip, String>(PackageTip.COMPARATOR, new TrivialMerger<Pair<ObjectUtils.Null, PackageTip>, String>());

    private static final Merger<Pair<ObjectUtils.Null, String>, PackageManifest> packageManifestMerger = new Merger<Pair<ObjectUtils.Null, String>, PackageManifest>() {
        @Override
        protected Triple<PackageManifest, PackageManifest, PackageManifest> mergeConflict(Context context, String label, Pair<ObjectUtils.Null, String> k, PackageManifest lhs, PackageManifest mhs, PackageManifest rhs) {
            Triple<Map<String, String>, Map<String, String>, Map<String, String>> mergedMetadata = metadataMerger.merge(context, combineLabel(label, "metadata"), ObjectUtils.NULL, lhs.metadata.toStringMap(), mhs.metadata.toStringMap(), rhs.metadata.toStringMap());
            Triple<Map<String, Pair<NormalDependencyType, String>>, Map<String, Pair<NormalDependencyType, String>>, Map<String, Pair<NormalDependencyType, String>>> mergedNormalDeps = normalDepsMerger.merge(context, combineLabel(label, "normalDeps"), ObjectUtils.NULL, lhs.normalDeps, mhs.normalDeps, rhs.normalDeps);
            Triple<Map<PackageTip, String>, Map<PackageTip, String>, Map<PackageTip, String>> mergedReplaceDeps = replaceDepsMerger.merge(context, combineLabel(label, "replaceDeps"), ObjectUtils.NULL, lhs.replaceDeps, mhs.replaceDeps, rhs.replaceDeps);
            PackageManifest newLhs = PackageManifest.of(PackageMetadataType.fromStringMap(mergedMetadata.getLeft()), mergedNormalDeps.getLeft(), mergedReplaceDeps.getLeft());
            PackageManifest newMhs = PackageManifest.of(PackageMetadataType.fromStringMap(mergedMetadata.getMiddle()), mergedNormalDeps.getMiddle(), mergedReplaceDeps.getMiddle());
            PackageManifest newRhs = PackageManifest.of(PackageMetadataType.fromStringMap(mergedMetadata.getRight()), mergedNormalDeps.getRight(), mergedReplaceDeps.getRight());
            return Triple.of(newLhs, newMhs, newRhs);
        }
    };

    private static final Merger<ObjectUtils.Null, Map<String, PackageManifest>> repoManifestMapMerger = new MapMerger<ObjectUtils.Null, String, PackageManifest>(Ordering.<String>natural(), packageManifestMerger);

    private static final Merger<Pair<ObjectUtils.Null, PackageTip>, RepoManifest> repoManifestMerger = new Merger<Pair<ObjectUtils.Null, PackageTip>, RepoManifest>() {
        @Override
        protected Triple<RepoManifest, RepoManifest, RepoManifest> mergeConflict(Context context, String label, Pair<ObjectUtils.Null, PackageTip> k, RepoManifest lhs, RepoManifest mhs, RepoManifest rhs) {
            PackageTip repo = k.getRight();
            Triple<VcsVersionDigest, VcsVersionDigest, VcsVersionDigest> mergedVersions = versionMerger.merge(context, combineLabel(label, "version"), repo, lhs.version, mhs.version, rhs.version);
            Triple<Map<String, PackageManifest>, Map<String, PackageManifest>, Map<String, PackageManifest>> mergedPackages = repoManifestMapMerger.merge(context, combineLabel(label, "packages"), ObjectUtils.NULL, lhs.packages, mhs.packages, rhs.packages);
            RepoManifest newLhs = RepoManifest.of(mergedVersions.getLeft(), mergedPackages.getLeft());
            RepoManifest newMhs = RepoManifest.of(mergedVersions.getMiddle(), mergedPackages.getMiddle());
            RepoManifest newRhs = RepoManifest.of(mergedVersions.getRight(), mergedPackages.getRight());
            return Triple.of(newLhs, newMhs, newRhs);
        }
    };

    private static final Merger<ObjectUtils.Null, Map<PackageTip, RepoManifest>> qbtManifestMapMerger = new MapMerger<ObjectUtils.Null, PackageTip, RepoManifest>(PackageTip.COMPARATOR, repoManifestMerger);

    private static final Merger<ObjectUtils.Null, QbtManifest> qbtManifestMerger = new Merger<ObjectUtils.Null, QbtManifest>() {
        @Override
        protected Triple<QbtManifest, QbtManifest, QbtManifest> mergeConflict(Context context, String label, ObjectUtils.Null k, QbtManifest lhs, QbtManifest mhs, QbtManifest rhs) {
            Triple<Map<PackageTip, RepoManifest>, Map<PackageTip, RepoManifest>, Map<PackageTip, RepoManifest>> merged = qbtManifestMapMerger.merge(context, label, ObjectUtils.NULL, lhs.repos, mhs.repos, rhs.repos);
            return Triple.of(QbtManifest.of(merged.getLeft()), QbtManifest.of(merged.getMiddle()), QbtManifest.of(merged.getRight()));
        }
    };
}
