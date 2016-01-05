package qbt.mains;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Map;
import misc1.commons.Maybe;
import misc1.commons.concurrent.ctree.ComputationTree;
import misc1.commons.options.NamedStringSingletonArgumentOptionsFragment;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import misc1.commons.ph.ProcessHelper;
import misc1.commons.resources.FreeScope;
import org.apache.commons.lang3.tuple.Pair;
import qbt.HelpTier;
import qbt.NormalDependencyType;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
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
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.ShellActionOptionsDelegate;
import qbt.options.ShellActionOptionsResult;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.recursive.cvrpd.CvRecursivePackageDataComputationMapper;
import qbt.recursive.cvrpd.CvRecursivePackageDataTransformer;
import qbt.recursive.utils.RecursiveDataUtils;
import qbt.tip.PackageTip;

public class RunPackage extends QbtCommand<RunPackage.Options> {
    @QbtCommandName("runPackage")
    public static interface Options extends QbtCommandOptions {
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public static final CumulativeVersionComputerOptionsDelegate<Options> cumulativeVersionComputerOptions = new CumulativeVersionComputerOptionsDelegate<Options>();
        public static final PackageMapperHelperOptionsDelegate<Options> packageMapperHelperOptions = new PackageMapperHelperOptionsDelegate<Options>();
        public static final OptionsFragment<Options, ?, String> pkg = new NamedStringSingletonArgumentOptionsFragment<Options>(ImmutableList.of("--package"), Maybe.<String>not(), "Run a command in this package");
        public static final ShellActionOptionsDelegate<Options> shellAction = new ShellActionOptionsDelegate<Options>();
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
        return "run a command in a package directory in an environment similar to a real build";
    }

    @Override
    public boolean isProgrammaticOutput() {
        return true;
    }

    @Override
    public int run(final OptionsResults<? extends Options> options) throws IOException {
        QbtConfig config = Options.config.getConfig(options);
        QbtManifest manifest = Options.manifest.getResult(options).parse();
        final CumulativeVersionComputerOptionsResult cumulativeVersionComputerOptionsResult = Options.cumulativeVersionComputerOptions.getResults(options);
        CumulativeVersionComputer<?> cumulativeVersionComputer = new BuildCumulativeVersionComputer(config, manifest) {
            @Override
            protected Map<String, String> getQbtEnv() {
                return cumulativeVersionComputerOptionsResult.qbtEnv;
            }
        };
        ShellActionOptionsResult shellActionOptionsResult = Options.shellAction.getResults(options);

        PackageTip packageTip = PackageTip.TYPE.parseRequire(options.get(Options.pkg));
        final CvRecursivePackageData<CumulativeVersionComputer.Result> r = cumulativeVersionComputer.compute(packageTip);

        try(final FreeScope scope = new FreeScope()) {
            final CvRecursivePackageDataTransformer<ArtifactReference, ArtifactReference> scopeReferenceTransformer = new CvRecursivePackageDataTransformer<ArtifactReference, ArtifactReference>() {
                @Override
                protected ArtifactReference transformResult(CumulativeVersion vOld, CumulativeVersion vNew, ArtifactReference result, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyResults) {
                    return result.copyInto(scope);
                }
            };
            Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyResults = PackageMapperHelper.run(config.artifactCacher, options, Options.packageMapperHelperOptions, new PackageMapperHelper.PackageMapperHelperCallback<Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>>>() {
                @Override
                public ComputationTree<Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>>> run(final PackageMapperHelper.PackageMapperHelperCallbackCallback cb) {
                    CvRecursivePackageDataComputationMapper<CumulativeVersionComputer.Result, CvRecursivePackageData<CumulativeVersionComputer.Result>, CvRecursivePackageData<ArtifactReference>> computationMapper = new CvRecursivePackageDataComputationMapper<CumulativeVersionComputer.Result, CvRecursivePackageData<CumulativeVersionComputer.Result>, CvRecursivePackageData<ArtifactReference>>() {
                        @Override
                        protected CvRecursivePackageData<ArtifactReference> map(CvRecursivePackageData<CumulativeVersionComputer.Result> commonRepoAccessor, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyResults) {
                            return new CvRecursivePackageData<ArtifactReference>(commonRepoAccessor.v, cb.runBuild(new BuildData(commonRepoAccessor, dependencyResults)), dependencyResults);
                        }
                    };

                    return RecursiveDataUtils.computationTreeMap(RecursiveDataUtils.transformMap(r.children, computationMapper.transformFunction), new Function<Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>>, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>>>() {
                        @Override
                        public Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> apply(Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> input) {
                            return RecursiveDataUtils.transformMap(input, scopeReferenceTransformer.transformFunction);
                        }
                    });
                }
            });
            BuildData bd = new BuildData(r, dependencyResults);
            BuildUtils.runPackageCommand(shellActionOptionsResult.commandArray, bd, new Function<ProcessHelper, Void>() {
                @Override
                public Void apply(ProcessHelper p) {
                    p = p.inheritInput();
                    p = p.inheritOutput();
                    p = p.inheritError();
                    p.run().requireSuccess();
                    return null;
                }
            });
            return 0;
        }
    }
}
