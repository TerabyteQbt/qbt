package qbt.mains;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Collection;
import misc1.commons.concurrent.ctree.ComputationTree;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import misc1.commons.options.UnparsedOptionsFragment;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.VcsVersionDigest;
import qbt.config.QbtConfig;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.ParallelismOptionsDelegate;
import qbt.options.RepoActionOptionsDelegate;
import qbt.remote.QbtRemote;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;

public class PushPins extends QbtCommand<PushPins.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PushPins.class);

    @QbtCommandName("pushPins")
    public static interface Options extends QbtCommandOptions {
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final ParallelismOptionsDelegate<Options> parallelism = new ParallelismOptionsDelegate<Options>();
        public static final RepoActionOptionsDelegate<Options> repos = new RepoActionOptionsDelegate<Options>(RepoActionOptionsDelegate.NoArgsBehaviour.OVERRIDES);
        public static final OptionsFragment<Options, ?, ImmutableList<String>> remotes = new UnparsedOptionsFragment<Options>("QBT remote to which to push", false, null, null);
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
        return "Push pins to remote qbt repositories";
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        final QbtConfig config = Options.config.getConfig(options);
        final QbtManifest manifest = Options.manifest.getResult(options).parse();
        final Collection<RepoTip> repos = Options.repos.getRepos(config, manifest, options);

        ComputationTree<?> computationTree = ComputationTree.list(Iterables.transform(options.get(Options.remotes), new Function<String, ComputationTree<ObjectUtils.Null>>() {
            @Override
            public ComputationTree<ObjectUtils.Null> apply(final String qbtRemoteString) {
                final QbtRemote qbtRemote = config.qbtRemoteFinder.requireQbtRemote(qbtRemoteString);
                return ComputationTree.transformIterable(repos, new Function<RepoTip, ObjectUtils.Null>() {
                    @Override
                    public ObjectUtils.Null apply(RepoTip repo) {
                        RepoManifest repoManifest = manifest.repos.get(repo);
                        if(repoManifest == null) {
                            throw new IllegalArgumentException("No such repo [tip]: " + repo);
                        }
                        VcsVersionDigest version = repoManifest.version;
                        PinnedRepoAccessor pinnedAccessor = config.localPinsRepo.requirePin(repo, version);
                        RawRemote remote = qbtRemote.findRemote(repo, true);

                        pinnedAccessor.pushToRemote(remote);

                        return ObjectUtils.NULL;
                    }
                }).ignore().transform(new Function<ObjectUtils.Null, ObjectUtils.Null>() {
                    @Override
                    public ObjectUtils.Null apply(ObjectUtils.Null input) {
                        LOGGER.info("Completed pushing to remote " + qbtRemoteString);
                        return input;
                    }
                });
            }
        }));

        Options.parallelism.getResult(options, false).runComputationTree(computationTree);
        return 0;
    }
}
