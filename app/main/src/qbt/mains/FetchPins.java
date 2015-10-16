package qbt.mains;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Collection;
import misc1.commons.concurrent.WorkPool;
import misc1.commons.concurrent.ctree.ComputationTree;
import misc1.commons.concurrent.ctree.ComputationTreeComputer;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import misc1.commons.options.UnparsedOptionsFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.QbtManifest;
import qbt.RepoManifest;
import qbt.VcsVersionDigest;
import qbt.config.QbtConfig;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.PackageRepoSelection;
import qbt.options.ParallelismOptionsDelegate;
import qbt.options.RepoActionOptionsDelegate;
import qbt.remote.QbtRemote;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;

public class FetchPins extends QbtCommand<FetchPins.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FetchPins.class);

    @QbtCommandName("fetchPins")
    public static interface Options extends QbtCommandOptions {
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final ParallelismOptionsDelegate<Options> parallelism = new ParallelismOptionsDelegate<Options>();
        public static final RepoActionOptionsDelegate<Options> repos = new RepoActionOptionsDelegate<Options>(new RepoActionOptionsDelegate.NoArgsBehaviour() {
            @Override
            public void run(ImmutableSet.Builder<RepoTip> b, QbtConfig config, QbtManifest manifest) {
                ImmutableSet<RepoTip> s0 = PackageRepoSelection.overrides(config, manifest);
                ImmutableSet<PackageTip> s1 = PackageRepoSelection.reposToPackages(manifest, s0);
                ImmutableSet<PackageTip> s2 = PackageRepoSelection.inwardsClosure(manifest, s1);
                ImmutableSet<RepoTip> s3 = PackageRepoSelection.packagesToRepos(manifest, s2);
                b.addAll(s3);
            }
        });
        public static final OptionsFragment<Options, ?, ImmutableList<String>> remote = new UnparsedOptionsFragment<Options>("QBT remote from which to fetch", false, 1, 1);
    }

    @Override
    public Class<Options> getOptionsClass() {
        return Options.class;
    }

    @Override
    public HelpTier getHelpTier() {
        return HelpTier.COMMON;
    }

    @Override
    public String getDescription() {
        return "Fetch pins from remote qbt repositories";
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        final QbtConfig config = Options.config.getConfig(options);
        final QbtManifest manifest = Options.manifest.getResult(options).parse();
        Collection<RepoTip> repos = Options.repos.getRepos(config, manifest, options);
        String qbtRemoteString = Iterables.getOnlyElement(options.get(Options.remote));
        final QbtRemote qbtRemote = config.qbtRemoteFinder.requireQbtRemote(qbtRemoteString);

        ComputationTree<ImmutableList<Boolean>> computationTree = ComputationTree.transformIterable(repos, new Function<RepoTip, Boolean>() {
            @Override
            public Boolean apply(RepoTip repo) {
                RepoManifest repoManifest = manifest.repos.get(repo);
                if(repoManifest == null) {
                    throw new IllegalArgumentException("No such repo [tip]: " + repo);
                }
                VcsVersionDigest version = repoManifest.version;

                if(config.localPinsRepo.findPin(repo, version) != null) {
                    LOGGER.debug("[" + repo + "] Already have " + version);
                    return true;
                }

                RawRemote remote = qbtRemote.findRemote(repo, false);
                LOGGER.info("[" + repo + "] Fetching from " + remote + "...");

                if(remote == null) {
                    LOGGER.debug("[" + repo + "] But repo does not exist, nothing to fetch");
                    return true;
                }

                config.localPinsRepo.fetchPins(repo, remote);

                if(config.localPinsRepo.findPin(repo, version) == null) {
                    LOGGER.error("[" + repo + "] But did not find " + version + "!");
                    return false;
                }

                return true;
            }
        });
        final WorkPool workPool = Options.parallelism.getResult(options, false).createWorkPool();
        ImmutableList<Boolean> oks;
        try {
            oks = new ComputationTreeComputer() {
                @Override
                protected void submit(Runnable r) {
                    workPool.submit(r);
                }
            }.await(computationTree).getCommute();
        }
        finally {
            workPool.shutdown();
        }
        for(Boolean ok : oks) {
            if(!ok) {
                return 1;
            }
        }
        return 0;
    }
}
