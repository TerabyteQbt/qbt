package qbt.mains;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import groovy.lang.GroovyShell;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import misc1.commons.ExceptionUtils;
import misc1.commons.Maybe;
import misc1.commons.Result;
import misc1.commons.algo.StronglyConnectedComponents;
import misc1.commons.concurrent.ctree.ComputationTree;
import misc1.commons.options.NamedBooleanFlagOptionsFragment;
import misc1.commons.options.NamedStringListArgumentOptionsFragment;
import misc1.commons.options.NamedStringSingletonArgumentOptionsFragment;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import misc1.commons.resources.FreeScope;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.NormalDependencyType;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.QbtTempDir;
import qbt.QbtUtils;
import qbt.artifactcacher.ArtifactReference;
import qbt.build.BuildData;
import qbt.build.BuildUtils;
import qbt.build.PackageMapperHelper;
import qbt.build.PackageMapperHelperOptionsDelegate;
import qbt.config.QbtConfig;
import qbt.manifest.current.QbtManifest;
import qbt.map.BuildCumulativeVersionComputer;
import qbt.map.CumulativeVersionComputer;
import qbt.map.CumulativeVersionComputerOptionsDelegate;
import qbt.map.CumulativeVersionComputerOptionsResult;
import qbt.map.DependencyComputer;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.PackageActionOptionsDelegate;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.recursive.cvrpd.CvRecursivePackageDataComputationMapper;
import qbt.recursive.srpd.SimpleRecursivePackageData;
import qbt.tip.PackageTip;

public final class BuildPlumbing extends QbtCommand<BuildPlumbing.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildPlumbing.class);

    public static interface BuildCommonOptions {
        public static final ConfigOptionsDelegate<BuildCommonOptions> config = new ConfigOptionsDelegate<BuildCommonOptions>();
        public static final ManifestOptionsDelegate<BuildCommonOptions> manifest = new ManifestOptionsDelegate<BuildCommonOptions>();
        public static final CumulativeVersionComputerOptionsDelegate<BuildCommonOptions> cumulativeVersionComputerOptions = new CumulativeVersionComputerOptionsDelegate<BuildCommonOptions>();
        public static final PackageMapperHelperOptionsDelegate<BuildCommonOptions> packageMapperHelperOptions = new PackageMapperHelperOptionsDelegate<BuildCommonOptions>();
        public static final OptionsFragment<BuildCommonOptions, ?, Boolean> shellOnError = new NamedBooleanFlagOptionsFragment<BuildCommonOptions>(ImmutableList.of("--shellOnError"), "Run a shell on failed builds");
        public static final OptionsFragment<BuildCommonOptions, ?, ImmutableList<String>> outputs = new NamedStringListArgumentOptionsFragment<BuildCommonOptions>(ImmutableList.of("--output"), "Output specifications");
        public static final OptionsFragment<BuildCommonOptions, ?, String> reports = new NamedStringSingletonArgumentOptionsFragment<BuildCommonOptions>(ImmutableList.of("--reports"), Maybe.<String>of(null), "Directory into which to dump reports");

        public static final OptionsFragment<BuildCommonOptions, ?, Boolean> verify = new NamedBooleanFlagOptionsFragment<BuildCommonOptions>(ImmutableList.of("--verify"), "Include all verify links");
        public static final OptionsFragment<BuildCommonOptions, ?, ImmutableList<String>> verifyTypes = new NamedStringListArgumentOptionsFragment<BuildCommonOptions>(ImmutableList.of("--verifyType"), "Include all verify links of this type");
        public static final OptionsFragment<BuildCommonOptions, ?, ImmutableList<String>> verifyRegexes = new NamedStringListArgumentOptionsFragment<BuildCommonOptions>(ImmutableList.of("--verifyRegex"), "Include all verify links for which \"from/type/to\" matches this regex");
        public static final OptionsFragment<BuildCommonOptions, ?, ImmutableList<String>> verifyGroovies = new NamedStringListArgumentOptionsFragment<BuildCommonOptions>(ImmutableList.of("--verifyGroovy"), "Include all verify links for which this groovy evaluates to true");
    }

    @QbtCommandName("buildPlumbing")
    public static interface Options extends BuildCommonOptions, QbtCommandOptions {
        public static final PackageActionOptionsDelegate<Options> packages = new PackageActionOptionsDelegate<Options>(PackageActionOptionsDelegate.NoArgsBehaviour.EMPTY);
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
        return "build packages (plumbing)";
    }

    private enum PublishTime {
        requested,
        all;
    }

    private enum PublishFormat {
        directory,
        tarball;
    }

    private static Triple<PublishTime, PublishFormat, String> parseOutput(String output) {
        int index1 = output.indexOf(',');
        if(index1 == -1) {
            throw new IllegalArgumentException("Could not understand --output: " + output);
        }
        int index2 = output.indexOf(',', index1 + 1);
        if(index2 == -1) {
            throw new IllegalArgumentException("Could not understand --output: " + output);
        }
        return Triple.of(PublishTime.valueOf(output.substring(0, index1)), PublishFormat.valueOf(output.substring(index1 + 1, index2)), output.substring(index2 + 1));
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        return run(options, Options.packages);
    }

    public static <O extends BuildCommonOptions> int run(final OptionsResults<? extends O> options, PackageActionOptionsDelegate<? super O> packagesOption) throws IOException {
        final QbtConfig config = BuildCommonOptions.config.getConfig(options);
        final QbtManifest manifest = BuildCommonOptions.manifest.getResult(options).parse();
        final Collection<PackageTip> packages = packagesOption.getPackages(config, manifest, options);

        ImmutableMultimap.Builder<PublishTime, Pair<PublishFormat, String>> outputsBuilder = ImmutableMultimap.builder();
        for(String output : options.get(Options.outputs)) {
            Triple<PublishTime, PublishFormat, String> parsedOutput = parseOutput(output);
            outputsBuilder.put(parsedOutput.getLeft(), Pair.of(parsedOutput.getMiddle(), parsedOutput.getRight()));
        }
        final ImmutableMultimap<PublishTime, Pair<PublishFormat, String>> outputs = outputsBuilder.build();

        final Predicate<Triple<PackageTip, String, PackageTip>> verifyPredicate = compileVerifyPredicate(options);

        final Path reportsDir;
        String reports = options.get(Options.reports);
        if(reports != null) {
            reportsDir = Paths.get(reports);
            QbtUtils.mkdirs(reportsDir);
        }
        else {
            reportsDir = null;
        }

        final CumulativeVersionComputerOptionsResult cumulativeVersionComputerOptionsResult = BuildCommonOptions.cumulativeVersionComputerOptions.getResults(options);

        final Lock shellLock = new ReentrantLock();
        PackageMapperHelper.run(config.artifactCacher, options, BuildCommonOptions.packageMapperHelperOptions, new PackageMapperHelper.PackageMapperHelperCallback<ObjectUtils.Null>() {
            private void runPublish(PublishTime time, PackageTip packageTip, CumulativeVersion v, ArtifactReference artifact) {
                for(Pair<PublishFormat, String> output : outputs.get(time)) {
                    String format = output.getRight();
                    format = format.replace("%p", v.getPackageName());
                    format = format.replace("%v", v.getDigest().getRawDigest().toString());
                    if(packageTip == null) {
                        if(format.contains("%t")) {
                            throw new IllegalArgumentException("--output contained %t for a publish with no tip");
                        }
                    }
                    else {
                        format = format.replace("%t", packageTip.tip);
                    }
                    Path f = Paths.get(format);
                    switch(output.getLeft()) {
                        case directory:
                            QbtUtils.mkdirs(f.getParent());
                            artifact.materializeDirectory(Maybe.<FreeScope>not(), f);
                            break;

                        case tarball:
                            QbtUtils.mkdirs(f.getParent());
                            artifact.materializeTarball(f);
                            break;

                        default:
                            throw new IllegalStateException();
                    }
                }
            }

            @Override
            public ComputationTree<ObjectUtils.Null> run(final PackageMapperHelper.PackageMapperHelperCallbackCallback cb) {
                final CvRecursivePackageDataComputationMapper<CumulativeVersionComputer.Result, CvRecursivePackageData<CumulativeVersionComputer.Result>, CvRecursivePackageData<ArtifactReference>> computationMapper = new CvRecursivePackageDataComputationMapper<CumulativeVersionComputer.Result, CvRecursivePackageData<CumulativeVersionComputer.Result>, CvRecursivePackageData<ArtifactReference>>() {
                    @Override
                    protected CvRecursivePackageData<ArtifactReference> map(CvRecursivePackageData<CumulativeVersionComputer.Result> commonRepoAccessor, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyResults) {
                        BuildData bd = new BuildData(commonRepoAccessor, dependencyResults);
                        CumulativeVersion v = commonRepoAccessor.v;
                        Pair<Result<ArtifactReference>, ArtifactReference> result = cb.runBuildFailable(bd);
                        ArtifactReference reports = result.getRight();
                        if(reports != null && reportsDir != null) {
                            Path destination = reportsDir.resolve(v.getPackageName() + "-" + v.getDigest().getRawDigest());
                            QbtUtils.mkdirs(destination.getParent());
                            reports.materializeDirectory(Maybe.<FreeScope>not(), destination);
                        }
                        Result<ArtifactReference> artifactResult = result.getLeft();
                        Throwable buildError = artifactResult.getThrowable();
                        if(buildError != null) {
                            if(options.get(Options.shellOnError)) {
                                shellLock.lock();
                                try {
                                    LOGGER.info("Invoking error shell for " + v.prettyDigest(), buildError);
                                    try(QbtTempDir tempDir = new QbtTempDir()) {
                                        final Path reportsDir = tempDir.resolve("reports");
                                        if(reports != null) {
                                            reports.materializeDirectory(Maybe.<FreeScope>not(), reportsDir);
                                        }
                                        BuildUtils.runPackageCommand(new String[] {System.getenv("SHELL")}, bd, (p) -> {
                                            p = p.putEnv("OUTPUT_REPORTS_DIR", reportsDir.toAbsolutePath().toString());
                                            p = p.inheritInput();
                                            p = p.inheritOutput();
                                            p = p.inheritError();
                                            p.run().requireSuccess();
                                            return null;
                                        });
                                    }
                                }
                                finally {
                                    shellLock.unlock();
                                }
                            }
                            throw ExceptionUtils.commute(buildError);
                        }
                        ArtifactReference artifactReference = artifactResult.getCommute();
                        runPublish(PublishTime.all, null, v, artifactReference);
                        return new CvRecursivePackageData<ArtifactReference>(commonRepoAccessor.v, artifactReference, dependencyResults);
                    }
                };
                final DependencyComputer dc = new DependencyComputer(manifest);
                final StronglyConnectedComponents<DependencyComputer.CacheKey> verifyComponents = new StronglyConnectedComponents<DependencyComputer.CacheKey>() {
                    @Override
                    protected Iterable<DependencyComputer.CacheKey> getLinks(DependencyComputer.CacheKey key) {
                        SimpleRecursivePackageData<DependencyComputer.Result> r = dc.compute(key);
                        ImmutableList.Builder<DependencyComputer.CacheKey> b = ImmutableList.builder();
                        for(Pair<NormalDependencyType, SimpleRecursivePackageData<DependencyComputer.Result>> e : r.children.values()) {
                            b.add(e.getRight().result.key);
                        }
                        for(Pair<PackageTip, String> verifyDep : r.result.packageManifest.verifyDeps) {
                            if(verifyPredicate.apply(Triple.of(r.result.packageTip, verifyDep.getRight(), verifyDep.getLeft()))) {
                                b.add(new DependencyComputer.CacheKey(verifyDep.getLeft(), r.result.replacementsNext));
                            }
                        }
                        return b.build();
                    }
                };
                final CumulativeVersionComputer<?> cumulativeVersionComputer = new BuildCumulativeVersionComputer(config, manifest) {
                    @Override
                    protected Map<String, String> getQbtEnv() {
                        return cumulativeVersionComputerOptionsResult.qbtEnv;
                    }
                };
                final LoadingCache<DependencyComputer.CacheKey, ComputationTree<CvRecursivePackageData<ArtifactReference>>> mainBuildTrees = CacheBuilder.newBuilder().build(new CacheLoader<DependencyComputer.CacheKey, ComputationTree<CvRecursivePackageData<ArtifactReference>>>() {
                    @Override
                    public ComputationTree<CvRecursivePackageData<ArtifactReference>> load(DependencyComputer.CacheKey key) {
                        CvRecursivePackageData<CumulativeVersionComputer.Result> r = cumulativeVersionComputer.compute(key);
                        return computationMapper.transform(r);
                    }
                });
                class VerifyTrees {
                    private final LoadingCache<StronglyConnectedComponents.Component<DependencyComputer.CacheKey>, ComputationTree<ObjectUtils.Null>> cache = CacheBuilder.newBuilder().build(new CacheLoader<StronglyConnectedComponents.Component<DependencyComputer.CacheKey>, ComputationTree<ObjectUtils.Null>>() {
                        @Override
                        public ComputationTree<ObjectUtils.Null> load(StronglyConnectedComponents.Component<DependencyComputer.CacheKey> c) {
                            ImmutableList.Builder<ComputationTree<ObjectUtils.Null>> b = ImmutableList.builder();
                            for(DependencyComputer.CacheKey key : c.vertices) {
                                b.add(mainBuildTrees.getUnchecked(key).ignore());
                            }
                            for(StronglyConnectedComponents.Component<DependencyComputer.CacheKey> c2 : verifyComponents.getLinks(c)) {
                                b.add(get(c2));
                            }
                            return ComputationTree.list(b.build()).ignore();
                        }
                    });

                    public ComputationTree<ObjectUtils.Null> get(StronglyConnectedComponents.Component<DependencyComputer.CacheKey> c) {
                        return cache.getUnchecked(c);
                    }
                }
                VerifyTrees verifyTrees = new VerifyTrees();

                ImmutableList.Builder<ComputationTree<ObjectUtils.Null>> computationTreesBuilder = ImmutableList.builder();
                for(final PackageTip packageTip : packages) {
                    DependencyComputer.CacheKey key = new DependencyComputer.CacheKey(packageTip);
                    StronglyConnectedComponents.Component<DependencyComputer.CacheKey> verifyComponent = verifyComponents.compute(key);
                    LOGGER.debug("Verify component: " + key + " -> " + verifyComponent.vertices);

                    ComputationTree<CvRecursivePackageData<ArtifactReference>> mainBuildTree = mainBuildTrees.getUnchecked(key);
                    mainBuildTree = mainBuildTree.transform((result) -> {
                        LOGGER.info("Built requested package " + packageTip + "@" + result.v.getDigest().getRawDigest());
                        return result;
                    });

                    ComputationTree<ObjectUtils.Null> verifyTree = verifyTrees.get(verifyComponent);

                    ComputationTree<CvRecursivePackageData<ArtifactReference>> verifiedTree = mainBuildTree.combineLeft(verifyTree);
                    verifiedTree = verifiedTree.transform((result) -> {
                        LOGGER.info("Verified requested package " + packageTip + "@" + result.v.getDigest().getRawDigest());
                        runPublish(PublishTime.requested, packageTip, result.v, result.result.getRight());
                        return result;
                    });

                    computationTreesBuilder.add(verifiedTree.ignore());
                }
                return ComputationTree.list(computationTreesBuilder.build()).ignore();
            }
        });
        return 0;
    }

    private static class VerifyNotingPredicate implements Predicate<Triple<PackageTip, String, PackageTip>> {
        private final String reason;
        private final Predicate<Triple<PackageTip, String, PackageTip>> delegate;
        private final Map<Triple<PackageTip, String, PackageTip>, Boolean> cache = Maps.newHashMap();

        public VerifyNotingPredicate(String reason, Predicate<Triple<PackageTip, String, PackageTip>> delegate) {
            this.reason = reason;
            this.delegate = delegate;
        }

        @Override
        public boolean apply(Triple<PackageTip, String, PackageTip> input) {
            Boolean already = cache.get(input);
            if(already != null) {
                return already;
            }

            boolean r = delegate.apply(input);
            if(r) {
                LOGGER.debug("Following verify link from " + input.getLeft() + " to " + input.getRight() + " type " + input.getMiddle() + " due to " + reason);
            }
            cache.put(input, r);

            return r;
        }
    }

    private static Predicate<Triple<PackageTip, String, PackageTip>> compileVerifyPredicate(OptionsResults<? extends BuildCommonOptions> options) {
        ImmutableList.Builder<Predicate<Triple<PackageTip, String, PackageTip>>> b = ImmutableList.builder();
        if(options.get(Options.verify)) {
            b.add(new VerifyNotingPredicate("(verify all)", Predicates.<Triple<PackageTip, String, PackageTip>>alwaysTrue()));
        }
        {
            ImmutableSet.Builder<String> verifyTypesBuilder = ImmutableSet.builder();
            for(String verifyTypeArgument : options.get(Options.verifyTypes)) {
                for(String verifyType : verifyTypeArgument.split(",")) {
                    verifyTypesBuilder.add(verifyType);
                }
            }
            final Set<String> verifyTypes = verifyTypesBuilder.build();
            if(!verifyTypes.isEmpty()) {
                b.add(new VerifyNotingPredicate("(verify type)", (input) -> verifyTypes.contains(input.getMiddle())));
            }
        }
        for(String verifyRegex : options.get(Options.verifyRegexes)) {
            final Pattern verifyPattern = Pattern.compile(verifyRegex);
            b.add(new VerifyNotingPredicate("(verify regex /" + verifyRegex + "/)", (input) -> verifyPattern.matcher(input.getLeft() + "/" + input.getMiddle() + "/" + input.getRight()).matches()));
        }
        for(final String verifyGroovy : options.get(Options.verifyGroovies)) {
            b.add(new VerifyNotingPredicate("(verify groovy `" + verifyGroovy + "`)", (input) -> {
                GroovyShell shell = new GroovyShell();
                shell.setVariable("from", input.getLeft());
                shell.setVariable("type", input.getMiddle());
                shell.setVariable("to", input.getRight());
                return (Boolean)shell.evaluate(verifyGroovy);
            }));
        }
        return Predicates.or(b.build());
    }
}
