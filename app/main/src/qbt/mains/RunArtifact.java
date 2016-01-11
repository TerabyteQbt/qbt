package qbt.mains;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import misc1.commons.Maybe;
import misc1.commons.concurrent.ctree.ComputationTree;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;
import misc1.commons.ph.ProcessHelper;
import misc1.commons.resources.FreeScope;
import org.apache.commons.lang3.tuple.Pair;
import qbt.HelpTier;
import qbt.NormalDependencyType;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.QbtTempDir;
import qbt.artifactcacher.ArtifactReference;
import qbt.build.BuildData;
import qbt.build.PackageMapperHelper;
import qbt.build.PackageMapperHelperOptionsDelegate;
import qbt.config.QbtConfig;
import qbt.manifest.current.QbtManifest;
import qbt.map.BuildCumulativeVersionComputer;
import qbt.map.CumulativeVersionComputer;
import qbt.map.CumulativeVersionComputerOptionsDelegate;
import qbt.map.CumulativeVersionComputerOptionsResult;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.recursive.cvrpd.CvRecursivePackageDataComputationMapper;
import qbt.tip.PackageTip;

public final class RunArtifact extends QbtCommand<RunArtifact.Options> {
    @QbtCommandName("runArtifact")
    public static interface Options extends QbtCommandOptions {
        public static final OptionsLibrary<Options> o = OptionsLibrary.of();
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final CumulativeVersionComputerOptionsDelegate<Options> cumulativeVersionComputerOptions = new CumulativeVersionComputerOptionsDelegate<Options>();
        public static final PackageMapperHelperOptionsDelegate<Options> packageMapperHelperOptions = new PackageMapperHelperOptionsDelegate<Options>();
        public static final OptionsFragment<Options, String> pkg = o.oneArg("package").transform(o.singleton()).helpDesc("Run an artifact from this");
        public static final OptionsFragment<Options, Boolean> absolute = o.zeroArg("absolute").transform(o.flag()).helpDesc("Do not prepend artifact path to command");
        public static final OptionsFragment<Options, Boolean> artifactsDir = o.zeroArg("artifactsDir").transform(o.flag()).helpDesc("Run the command in the package artifact directory rather than the current directory");
        public static final OptionsFragment<Options, ImmutableList<String>> command = o.unparsed(true).transform(o.min(1)).helpDesc("Command to run (first argument is relative to artifact by default)");
    }

    @Override
    public Class<Options> getOptionsClass() {
        return Options.class;
    }

    @Override
    public HelpTier getHelpTier() {
        return HelpTier.UNCOMMON;
    }

    @Override
    public String getDescription() {
        return "run a command from \"within\" a built package's artifact";
    }

    @Override
    public boolean isProgrammaticOutput() {
        return true;
    }

    @Override
    public int run(final OptionsResults<? extends Options> options) throws IOException {
        final QbtConfig config = Options.config.getConfig(options);
        final QbtManifest manifest = Options.manifest.getResult(options).parse();
        final PackageTip packageTip = PackageTip.TYPE.parseRequire(options.get(Options.pkg));
        final CumulativeVersionComputerOptionsResult cumulativeVersionComputerOptionsResult = Options.cumulativeVersionComputerOptions.getResults(options);

        try(final FreeScope scope = new FreeScope()) {
            ArtifactReference result = PackageMapperHelper.run(config.artifactCacher, options, Options.packageMapperHelperOptions, new PackageMapperHelper.PackageMapperHelperCallback<ArtifactReference>() {
                @Override
                public ComputationTree<ArtifactReference> run(final PackageMapperHelper.PackageMapperHelperCallbackCallback cb) {
                    CvRecursivePackageDataComputationMapper<CumulativeVersionComputer.Result, CvRecursivePackageData<CumulativeVersionComputer.Result>, CvRecursivePackageData<ArtifactReference>> computationMapper = new CvRecursivePackageDataComputationMapper<CumulativeVersionComputer.Result, CvRecursivePackageData<CumulativeVersionComputer.Result>, CvRecursivePackageData<ArtifactReference>>() {
                        @Override
                        protected CvRecursivePackageData<ArtifactReference> map(CvRecursivePackageData<CumulativeVersionComputer.Result> commonRepoAccessor, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyResults) {
                            return new CvRecursivePackageData<ArtifactReference>(commonRepoAccessor.v, cb.runBuild(new BuildData(commonRepoAccessor, dependencyResults)), dependencyResults);
                        }
                    };

                    CumulativeVersionComputer<?> cumulativeVersionComputer = new BuildCumulativeVersionComputer(config, manifest) {
                        @Override
                        protected Map<String, String> getQbtEnv() {
                            return cumulativeVersionComputerOptionsResult.qbtEnv;
                        }
                    };
                    return computationMapper.transform(cumulativeVersionComputer.compute(packageTip)).transform((result) -> result.result.getRight().copyInto(scope));
                }
            });

            try(QbtTempDir tempDir = new QbtTempDir()) {
                Path outputsDir = tempDir.resolve("artifact");
                result.materializeDirectory(Maybe.of(scope), outputsDir);

                String[] args = options.get(Options.command).toArray(new String[0]);
                if(!options.get(Options.absolute)) {
                    args[0] = outputsDir.resolve(args[0]).toString();
                }
                Path dir = options.get(Options.artifactsDir) ? outputsDir : Paths.get(".");
                ProcessHelper p = ProcessHelper.of(dir, args);
                p = p.putEnv("ARTIFACTS_DIR", outputsDir.toString());
                p = p.inheritError();
                p = p.inheritInput();
                p = p.inheritOutput();
                return p.run().exitCode;
            }
        }
    }
}
