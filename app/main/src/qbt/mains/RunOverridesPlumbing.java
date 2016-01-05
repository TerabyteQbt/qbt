package qbt.mains;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import misc1.commons.concurrent.ctree.ComputationTree;
import misc1.commons.options.NamedBooleanFlagOptionsFragment;
import misc1.commons.options.NamedStringListArgumentOptionsFragment;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import misc1.commons.ph.ProcessHelper;
import org.apache.commons.lang3.ObjectUtils;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.QbtUtils;
import qbt.VcsVersionDigest;
import qbt.config.QbtConfig;
import qbt.manifest.QbtManifestVersions;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.ParallelismOptionsDelegate;
import qbt.options.RepoActionOptionsDelegate;
import qbt.options.ShellActionOptionsDelegate;
import qbt.options.ShellActionOptionsResult;
import qbt.repo.LocalRepoAccessor;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.utils.ProcessHelperUtils;

public final class RunOverridesPlumbing extends QbtCommand<RunOverridesPlumbing.Options> {
    public static interface RunOverridesCommonOptions {
        public static final ConfigOptionsDelegate<RunOverridesCommonOptions> config = new ConfigOptionsDelegate<RunOverridesCommonOptions>();
        public static final ManifestOptionsDelegate<RunOverridesCommonOptions> manifest = new ManifestOptionsDelegate<RunOverridesCommonOptions>();
        public static final ParallelismOptionsDelegate<RunOverridesCommonOptions> parallelism = new ParallelismOptionsDelegate<RunOverridesCommonOptions>();
        public static final OptionsFragment<RunOverridesCommonOptions, ?, Boolean> noPrefix = new NamedBooleanFlagOptionsFragment<RunOverridesCommonOptions>(ImmutableList.of("--no-prefix"), "Don't prefix each line of output with repo banner (the default is to prefix)");
        public static final OptionsFragment<RunOverridesCommonOptions, ?, ImmutableList<String>> extraManifests = new NamedStringListArgumentOptionsFragment<RunOverridesCommonOptions>(ImmutableList.of("--extra-manifest"), "Extra QBT manifest file (<name>=<filename>)");
        public static final ShellActionOptionsDelegate<RunOverridesCommonOptions> shellAction = new ShellActionOptionsDelegate<RunOverridesCommonOptions>();
    }

    @QbtCommandName("runOverridesPlumbing")
    public static interface Options extends RunOverridesCommonOptions, QbtCommandOptions {
        public static final RepoActionOptionsDelegate<Options> repos = new RepoActionOptionsDelegate<Options>(RepoActionOptionsDelegate.NoArgsBehaviour.EMPTY);
    }

    @Override
    public Class<Options> getOptionsClass() {
        return Options.class;
    }

    @Override
    public HelpTier getHelpTier() {
        return HelpTier.PLUMBING;
    }

    @Override
    public String getDescription() {
        return "run a command in all overrides (plumbing)";
    }

    @Override
    public boolean isProgrammaticOutput() {
        return true;
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        return run(options, Options.repos);
    }

    public static <O extends RunOverridesCommonOptions> int run(final OptionsResults<? extends O> options, RepoActionOptionsDelegate<? super O> reposOption) throws IOException {
        final QbtConfig config = RunOverridesCommonOptions.config.getConfig(options);
        final QbtManifest manifest = RunOverridesCommonOptions.manifest.getResult(options).parse();
        Collection<RepoTip> repos = reposOption.getRepos(config, manifest, options);
        final ShellActionOptionsResult shellActionOptionsResult = RunOverridesCommonOptions.shellAction.getResults(options);

        ImmutableMap.Builder<String, QbtManifest> extraManifestsBuilder = ImmutableMap.builder();
        for(String arg : options.get(Options.extraManifests)) {
            int idx = arg.indexOf('=');
            if(idx == -1) {
                throw new IllegalArgumentException("Invalid --extra-manifest: " + arg);
            }
            extraManifestsBuilder.put(arg.substring(0, idx), QbtManifestVersions.parse(QbtUtils.readLines(Paths.get(arg.substring(idx + 1)))));
        }
        final Map<String, QbtManifest> extraManifests = extraManifestsBuilder.build();

        ComputationTree<?> computationTree = ComputationTree.transformIterable(repos, new Function<RepoTip, ObjectUtils.Null>() {
            @Override
            public ObjectUtils.Null apply(final RepoTip repo) {
                RepoManifest repoManifest = manifest.repos.get(repo);
                if(repoManifest == null) {
                    throw new IllegalArgumentException("No such repo [tip]: " + repo);
                }
                VcsVersionDigest version = repoManifest.version;
                final LocalRepoAccessor localRepoAccessor = config.localRepoFinder.findLocalRepo(repo);
                if(localRepoAccessor == null) {
                    return ObjectUtils.NULL;
                }
                ProcessHelper p = ProcessHelper.of(localRepoAccessor.dir, shellActionOptionsResult.commandArray);
                class VersionAdder {
                    public ProcessHelper addVersion(ProcessHelper p, String name, VcsVersionDigest version) {
                        String envName = "REPO_VERSION" + (name == null ? "" : ("_" + name));
                        if(version != null) {
                            if(!localRepoAccessor.vcs.getRepository(localRepoAccessor.dir).commitExists(version)) {
                                PinnedRepoAccessor pinnedAccessor = config.localPinsRepo.requirePin(repo, version);
                                pinnedAccessor.findCommit(localRepoAccessor.dir);
                            }
                        }
                        p = p.putEnv(envName, version == null ? "" : version.getRawDigest().toString());
                        return p;
                    }
                }
                VersionAdder va = new VersionAdder(); // exists only as a dirty trick to avoid a static addVersion() with a thousand arguments
                p = p.putEnv("REPO_NAME", repo.name);
                p = p.putEnv("REPO_TIP", repo.tip);
                p = va.addVersion(p, null, version);
                for(Map.Entry<String, QbtManifest> e : extraManifests.entrySet()) {
                    String manifestName = e.getKey();
                    RepoManifest extraRepoManifest = e.getValue().repos.get(repo);
                    p = va.addVersion(p, manifestName, extraRepoManifest == null ? null : extraRepoManifest.version);
                }
                if(shellActionOptionsResult.isInteractive) {
                    p = p.inheritInput();
                    p = p.inheritOutput();
                    p = p.inheritError();
                    p.run().requireSuccess();
                }
                else if(options.get(Options.noPrefix)) {
                    p = p.inheritOutput();
                    p = p.inheritError();
                    p.run().requireSuccess();
                }
                else {
                    p.run(ProcessHelperUtils.simplePrefixCallback(String.valueOf(repo)));
                }
                return ObjectUtils.NULL;
            }
        });
        RunOverridesCommonOptions.parallelism.getResult(options, shellActionOptionsResult.isInteractive).runComputationTree(computationTree);
        return 0;
    }
}
