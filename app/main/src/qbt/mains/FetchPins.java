package qbt.mains;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import misc1.commons.concurrent.ctree.ComputationTree;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;
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
        public static final OptionsLibrary<Options> o = OptionsLibrary.of();
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final ParallelismOptionsDelegate<Options> parallelism = new ParallelismOptionsDelegate<Options>();
        public static final RepoActionOptionsDelegate<Options> repos = new RepoActionOptionsDelegate<Options>((b, config, manifest) -> {
            ImmutableSet<RepoTip> s0 = PackageRepoSelection.overrides(config, manifest);
            ImmutableSet<PackageTip> s1 = PackageRepoSelection.reposToPackages(manifest, s0);
            ImmutableSet<PackageTip> s2 = PackageRepoSelection.inwardsClosure(manifest, s1);
            ImmutableSet<RepoTip> s3 = PackageRepoSelection.packagesToRepos(manifest, s2);
            b.addAll(s3);
        });
        public static final OptionsFragment<Options, String> remote = o.unparsed(false).transform(o.singleton()).helpDesc("QBT remote from which to fetch");
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
        final String qbtRemoteString = options.get(Options.remote);
        final QbtRemote qbtRemote = config.qbtRemoteFinder.requireQbtRemote(qbtRemoteString);

        ComputationTree<?> computationTree = ComputationTree.transformIterable(repos, (repo) -> {
            RepoManifest repoManifest = manifest.repos.get(repo);
            if(repoManifest == null) {
                throw new IllegalArgumentException("No such repo [tip]: " + repo);
            }
            Optional<VcsVersionDigest> maybeVersion = repoManifest.___version;
            if(!maybeVersion.isPresent()) {
                LOGGER.debug("[" + repo + "] Doesn't have version");
                return ObjectUtils.NULL;
            }
            VcsVersionDigest version = maybeVersion.get();
            if(config.localPinsRepo.findPin(repo, version) != null) {
                LOGGER.debug("[" + repo + "] Already have " + version);
                return ObjectUtils.NULL;
            }

            RawRemote remote = qbtRemote.findRemote(repo, false);

            if(remote != null) {
                LOGGER.info("[" + repo + "] Fetching from " + remote + "...");
                config.localPinsRepo.fetchPins(repo, remote);
            }
            else {
                LOGGER.info("[" + repo + "] Repo does not exist in remote '" + qbtRemoteString + "'");
            }

            config.localPinsRepo.requirePin(repo, version, "[" + repo + "] Could not find " + version + "!");
            return ObjectUtils.NULL;
        });

        Options.parallelism.getResult(options, false).runComputationTree(computationTree);
        return 0;
    }
}
