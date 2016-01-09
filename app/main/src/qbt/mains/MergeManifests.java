package qbt.mains;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import misc1.commons.Maybe;
import misc1.commons.options.NamedEnumSingletonArgumentOptionsFragment;
import misc1.commons.options.NamedStringSingletonArgumentOptionsFragment;
import misc1.commons.options.OptionsException;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import misc1.commons.ph.ProcessHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.QbtTempDir;
import qbt.VcsVersionDigest;
import qbt.config.QbtConfig;
import qbt.manifest.LegacyQbtManifest;
import qbt.manifest.QbtManifestVersion;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.ShellActionOptionsDelegate;
import qbt.options.ShellActionOptionsResult;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.utils.ProcessHelperUtils;
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
        void invoke(RepoTip repo, Repository repository, VcsVersionDigest lhs, VcsVersionDigest mhs, VcsVersionDigest rhs);
    }

    public static enum StrategyEnum implements Strategy {
        merge {
            @Override
            public void invoke(RepoTip repo, Repository repository, VcsVersionDigest lhs, VcsVersionDigest mhs, VcsVersionDigest rhs) {
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
            public void invoke(RepoTip repo, Repository repository, VcsVersionDigest lhs, VcsVersionDigest mhs, VcsVersionDigest rhs) {
                repository.checkout(lhs);
                repository.rebase(mhs, rhs);
            }
        };
    }

    private static Strategy parseStrategyOptions(OptionsResults<? extends Options> options) {
        ImmutableList.Builder<Strategy> b = ImmutableList.builder();
        final ShellActionOptionsResult shellActionOptionsResult = Options.shellAction.getResultsOptional(options);
        if(shellActionOptionsResult != null) {
            b.add((repo, repository, lhs, mhs, rhs) -> {
                repository.checkout(lhs);
                ProcessHelper p = ProcessHelper.of(repository.getRoot(), shellActionOptionsResult.commandArray);
                p = p.apply(ProcessHelperUtils::stripGitEnv);
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
                    p.run().requireSuccess();
                }
                else {
                    p.run(ProcessHelperUtils.simplePrefixCallback(String.valueOf(repo)));
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

    public static QbtManifestVersion<?, ?> chooseTargetVersion(LegacyQbtManifest<?, ?> lhs, LegacyQbtManifest<?, ?> mhs, LegacyQbtManifest<?, ?> rhs) {
        // These had both better be upgrades or you're doing something bad
        // (e.g.  CP of something in new format onto old format branch)
        mhs.upgrade(lhs.version);
        mhs.upgrade(rhs.version);

        return lhs.version.max(rhs.version);
    }

    @Override
    public int run(final OptionsResults<? extends Options> options) throws IOException {
        final QbtConfig config = Options.config.getConfig(options);
        final Strategy strategy = parseStrategyOptions(options);

        final LegacyQbtManifest<?, ?> lhsLegacyResult = Options.lhs.getResult(options).parseLegacy();
        final LegacyQbtManifest<?, ?> mhsLegacyResult = Options.mhs.getResult(options).parseLegacy();
        final LegacyQbtManifest<?, ?> rhsLegacyResult = Options.rhs.getResult(options).parseLegacy();
        final String lhsName = options.get(Options.lhsName);
        final String mhsName = options.get(Options.mhsName);
        final String rhsName = options.get(Options.rhsName);
        QbtManifestVersion<?, ?> targetVersion = chooseTargetVersion(lhsLegacyResult, mhsLegacyResult, rhsLegacyResult);
        return new Object() {
            public <M, B> int run(QbtManifestVersion<M, B> targetVersion) {
                M lhs = lhsLegacyResult.upgrade(targetVersion).manifest;
                M mhs = mhsLegacyResult.upgrade(targetVersion).manifest;
                M rhs = rhsLegacyResult.upgrade(targetVersion).manifest;

                // First: merge anything automatic
                {
                    Triple<M, M, M> merged = targetVersion.merge().merge(lhs, mhs, rhs);
                    lhs = merged.getLeft();
                    mhs = merged.getMiddle();
                    rhs = merged.getRight();
                }

                QbtManifest lhsCurrent = targetVersion.current(lhs);
                QbtManifest mhsCurrent = targetVersion.current(mhs);
                QbtManifest rhsCurrent = targetVersion.current(rhs);

                // Second: EDIT/EDIT conflicts in version are put through strategy
                ImmutableSet.Builder<RepoTip> repos = ImmutableSet.builder();
                repos.addAll(lhsCurrent.repos.keySet());
                repos.addAll(mhsCurrent.repos.keySet());
                repos.addAll(rhsCurrent.repos.keySet());

                B lhsBuilder = targetVersion.builder(lhs);
                B mhsBuilder = targetVersion.builder(mhs);
                B rhsBuilder = targetVersion.builder(rhs);
                for(RepoTip repo : repos.build()) {
                    RepoManifest lhsRepoManifest = lhsCurrent.repos.get(repo);
                    if(lhsRepoManifest == null) {
                        continue;
                    }
                    RepoManifest mhsRepoManifest = mhsCurrent.repos.get(repo);
                    if(mhsRepoManifest == null) {
                        continue;
                    }
                    RepoManifest rhsRepoManifest = rhsCurrent.repos.get(repo);
                    if(rhsRepoManifest == null) {
                        continue;
                    }

                    VcsVersionDigest lhsVersion = lhsRepoManifest.version;
                    VcsVersionDigest mhsVersion = mhsRepoManifest.version;
                    VcsVersionDigest rhsVersion = rhsRepoManifest.version;

                    if(lhsVersion.equals(mhsVersion) && mhsVersion.equals(rhsVersion)) {
                        continue;
                    }

                    PinnedRepoAccessor lhsResult = config.localPinsRepo.requirePin(repo, lhsVersion);
                    LocalVcs lhsLocalVcs = lhsResult.getLocalVcs();

                    PinnedRepoAccessor mhsResult = config.localPinsRepo.requirePin(repo, mhsVersion);
                    LocalVcs mhsLocalVcs = mhsResult.getLocalVcs();

                    PinnedRepoAccessor rhsResult = config.localPinsRepo.requirePin(repo, rhsVersion);
                    LocalVcs rhsLocalVcs = rhsResult.getLocalVcs();

                    if(!lhsLocalVcs.equals(mhsLocalVcs) || !rhsLocalVcs.equals(mhsLocalVcs)) {
                        LOGGER.error("[" + repo + "] Found mis-matched VCS: " + lhsLocalVcs + ", " + mhsLocalVcs + ", " + rhsLocalVcs);
                        continue;
                    }
                    LocalVcs localVcs = lhsLocalVcs;

                    VcsVersionDigest result;
                    try(QbtTempDir tempDir = new QbtTempDir()) {
                        Path repoDir = tempDir.path;
                        localVcs.createWorkingRepo(repoDir);
                        lhsResult.findCommit(repoDir);
                        mhsResult.findCommit(repoDir);
                        rhsResult.findCommit(repoDir);
                        Repository repository = localVcs.getRepository(repoDir);
                        try {
                            strategy.invoke(repo, repository, lhsVersion, mhsVersion, rhsVersion);
                        }
                        catch(RuntimeException e) {
                            LOGGER.error("[" + repo + "]", e);
                            continue;
                        }
                        result = repository.getCurrentCommit();
                        lhsResult.addPin(repoDir, result);
                        rhsResult.addPin(repoDir, result);
                    }

                    lhsBuilder = targetVersion.withRepoVersion(lhsBuilder, repo, result);
                    mhsBuilder = targetVersion.withRepoVersion(mhsBuilder, repo, result);
                    rhsBuilder = targetVersion.withRepoVersion(rhsBuilder, repo, result);
                }
                M lhsMerged = targetVersion.build(lhsBuilder);
                M mhsMerged = targetVersion.build(mhsBuilder);
                M rhsMerged = targetVersion.build(rhsBuilder);

                ImmutableList<Pair<String, String>> conflicts = Options.out.getResult(options).deparseConflict(targetVersion, lhsName, lhsMerged, mhsName, mhsMerged, rhsName, rhsMerged);

                for(Pair<String, String> conflict : conflicts) {
                    LOGGER.error(conflict.getRight() + " conflict at " + conflict.getLeft());
                }

                return conflicts.isEmpty() ? 0 : 1;
            }
        }.run(targetVersion);
    }
}
