package qbt.mains;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.HashCode;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import misc1.commons.ExceptionUtils;
import misc1.commons.Maybe;
import misc1.commons.Result;
import misc1.commons.concurrent.ctree.ComputationTree;
import misc1.commons.options.NamedBooleanFlagOptionsFragment;
import misc1.commons.options.NamedStringListArgumentOptionsFragment;
import misc1.commons.options.NamedStringSingletonArgumentOptionsFragment;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.NormalDependencyType;
import qbt.PackageTip;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.QbtHashUtils;
import qbt.QbtManifest;
import qbt.QbtUtils;
import qbt.artifactcacher.ArtifactReference;
import qbt.build.BuildData;
import qbt.build.BuildUtils;
import qbt.build.PackageMapperHelper;
import qbt.build.PackageMapperHelperOptionsDelegate;
import qbt.config.QbtConfig;
import qbt.map.BuildManifestCumulativeVersionComputer;
import qbt.map.CumulativeVersionComputer;
import qbt.map.CumulativeVersionComputerOptionsDelegate;
import qbt.map.CumulativeVersionComputerOptionsResult;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.PackageActionOptionsDelegate;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.recursive.cvrpd.CvRecursivePackageDataComputationMapper;
import qbt.utils.ProcessHelper;

public final class BuildPlumbing extends QbtCommand<BuildPlumbing.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildPlumbing.class);

    public static interface BuildCommonOptions {
        public static final ConfigOptionsDelegate<BuildCommonOptions> config = new ConfigOptionsDelegate<BuildCommonOptions>();
        public static final ManifestOptionsDelegate<BuildCommonOptions> manifest = new ManifestOptionsDelegate<BuildCommonOptions>();
        public static final CumulativeVersionComputerOptionsDelegate<BuildCommonOptions> cumulativeVersionComputerOptions = new CumulativeVersionComputerOptionsDelegate<BuildCommonOptions>();
        public static final PackageMapperHelperOptionsDelegate<BuildCommonOptions> packageMapperHelperOptions = new PackageMapperHelperOptionsDelegate<BuildCommonOptions>();
        public static final OptionsFragment<BuildCommonOptions, ?, ImmutableList<String>> skips = new NamedStringListArgumentOptionsFragment<BuildCommonOptions>(ImmutableList.of("--skip"), "Skip this package/cumulativeVersion");
        public static final OptionsFragment<BuildCommonOptions, ?, ImmutableList<String>> skipsFromFiles = new NamedStringListArgumentOptionsFragment<BuildCommonOptions>(ImmutableList.of("--skipFromFile"), "Read --skip arguments from this file");
        public static final OptionsFragment<BuildCommonOptions, ?, Boolean> shellOnError = new NamedBooleanFlagOptionsFragment<BuildCommonOptions>(ImmutableList.of("--shellOnError"), "Run a shell on failed builds");
        public static final OptionsFragment<BuildCommonOptions, ?, ImmutableList<String>> outputs = new NamedStringListArgumentOptionsFragment<BuildCommonOptions>(ImmutableList.of("--output"), "Output specifications");
        public static final OptionsFragment<BuildCommonOptions, ?, String> reports = new NamedStringSingletonArgumentOptionsFragment<BuildCommonOptions>(ImmutableList.of("--reports"), Maybe.<String>of(null), "Directory into which to dump reports");
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
        class SkipsBuilder {
            private final ImmutableSet.Builder<Pair<PackageTip, HashCode>> b = ImmutableSet.builder();

            private void addSkip(String arg) {
                String[] pieces = arg.split(" ");
                if(pieces.length != 2) {
                    throw new IllegalArgumentException("Bad skip: " + arg);
                }
                b.add(Pair.of(PackageTip.parseRequire(pieces[0], "package"), QbtHashUtils.parse(pieces[1])));
            }

            private void addSkipsFromFile(String arg) throws IOException {
                for(String line : QbtUtils.readLines(Paths.get(arg))) {
                    addSkip(line);
                }
            }
        }
        SkipsBuilder skipsBuilder = new SkipsBuilder();
        for(String arg : options.get(Options.skips)) {
            skipsBuilder.addSkip(arg);
        }
        for(String arg : options.get(Options.skipsFromFiles)) {
            skipsBuilder.addSkipsFromFile(arg);
        }
        final ImmutableSet<Pair<PackageTip, HashCode>> skips = skipsBuilder.b.build();

        ImmutableMultimap.Builder<PublishTime, Pair<PublishFormat, String>> outputsBuilder = ImmutableMultimap.builder();
        for(String output : options.get(Options.outputs)) {
            Triple<PublishTime, PublishFormat, String> parsedOutput = parseOutput(output);
            outputsBuilder.put(parsedOutput.getLeft(), Pair.of(parsedOutput.getMiddle(), parsedOutput.getRight()));
        }
        final ImmutableMultimap<PublishTime, Pair<PublishFormat, String>> outputs = outputsBuilder.build();

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
                            QbtUtils.mkdirs(f);
                            artifact.materializeDirectory(f);
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
                CvRecursivePackageDataComputationMapper<CumulativeVersionComputer.Result, CvRecursivePackageData<CumulativeVersionComputer.Result>, CvRecursivePackageData<ArtifactReference>> computationMapper = new CvRecursivePackageDataComputationMapper<CumulativeVersionComputer.Result, CvRecursivePackageData<CumulativeVersionComputer.Result>, CvRecursivePackageData<ArtifactReference>>() {
                    @Override
                    protected CvRecursivePackageData<ArtifactReference> map(CvRecursivePackageData<CumulativeVersionComputer.Result> requireRepoResults, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyResults) {
                        BuildData bd = new BuildData(requireRepoResults, dependencyResults);
                        CumulativeVersion v = requireRepoResults.v;
                        Pair<Result<ArtifactReference>, ArtifactReference> result = cb.runBuildFailable(bd);
                        ArtifactReference reports = result.getRight();
                        if(reports != null && reportsDir != null) {
                            Path destination = reportsDir.resolve(v.getPackageName() + "-" + v.getDigest().getRawDigest());
                            QbtUtils.mkdirs(destination);
                            reports.materializeDirectory(destination);
                        }
                        Result<ArtifactReference> artifactResult = result.getLeft();
                        Throwable buildError = artifactResult.getThrowable();
                        if(buildError != null) {
                            if(options.get(Options.shellOnError)) {
                                shellLock.lock();
                                try {
                                    LOGGER.info("Invoking error shell for " + v.prettyDigest(), buildError);
                                    BuildUtils.runPackageCommand(new String[] {System.getenv("SHELL")}, bd, new Function<ProcessHelper, Void>() {
                                        @Override
                                        public Void apply(ProcessHelper p) {
                                            p = p.inheritInput();
                                            p = p.inheritOutput();
                                            p = p.inheritError();
                                            p.completeVoid();
                                            return null;
                                        }
                                    });
                                }
                                finally {
                                    shellLock.unlock();
                                }
                            }
                            throw ExceptionUtils.commute(buildError);
                        }
                        ArtifactReference artifactReference = artifactResult.getCommute();
                        runPublish(PublishTime.all, null, v, artifactReference);
                        return new CvRecursivePackageData<ArtifactReference>(requireRepoResults.v, artifactReference, dependencyResults);
                    }
                };

                ImmutableList.Builder<ComputationTree<ObjectUtils.Null>> computationTreesBuilder = ImmutableList.builder();
                CumulativeVersionComputer<?> cumulativeVersionComputer = new BuildManifestCumulativeVersionComputer(config, manifest) {
                    @Override
                    protected Map<String, String> getQbtEnv() {
                        return cumulativeVersionComputerOptionsResult.qbtEnv;
                    }
                };
                for(final PackageTip packageTip : packages) {
                    CvRecursivePackageData<CumulativeVersionComputer.Result> r = cumulativeVersionComputer.compute(packageTip);
                    if(skips.contains(Pair.of(packageTip, r.v.getDigest().getRawDigest()))) {
                        continue;
                    }
                    computationTreesBuilder.add(computationMapper.transform(r).transform(new Function<CvRecursivePackageData<ArtifactReference>, ObjectUtils.Null>() {
                        @Override
                        public ObjectUtils.Null apply(CvRecursivePackageData<ArtifactReference> result) {
                            LOGGER.info("Completed request package " + packageTip + "@" + result.v.getDigest().getRawDigest());
                            runPublish(PublishTime.requested, packageTip, result.v, result.result.getRight());
                            return ObjectUtils.NULL;
                        }
                    }));
                }
                return ComputationTree.list(computationTreesBuilder.build()).ignore();
            }
        });
        return 0;
    }
}
