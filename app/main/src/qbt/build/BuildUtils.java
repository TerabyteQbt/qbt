package qbt.build;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import misc1.commons.Maybe;
import misc1.commons.Result;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.NormalDependencyType;
import qbt.PackageDirectories;
import qbt.PackageDirectory;
import qbt.QbtTempDir;
import qbt.QbtUtils;
import qbt.artifactcacher.ArtifactReference;
import qbt.artifactcacher.ArtifactScope;
import qbt.metadata.PackageBuildType;
import qbt.metadata.PackageMetadataType;
import qbt.recursive.cvrpd.CvRecursivePackageData;
import qbt.recursive.cvrpd.CvRecursivePackageDataMapper;
import qbt.utils.ProcessHelper;
import qbt.utils.TarballUtils;

public final class BuildUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildUtils.class);

    private BuildUtils() {
        // no
    }

    public static void materializeArtifact(Path dir, ArtifactReference artifact) {
        QbtUtils.mkdirs(dir);
        artifact.materializeDirectory(dir);
    }

    private static class StrongDependencyMaterializer extends CvRecursivePackageDataMapper<ArtifactReference, ObjectUtils.Null> {
        private final Path root;

        public StrongDependencyMaterializer(Path root) {
            this.root = root;
        }

        @Override
        protected ObjectUtils.Null map(CvRecursivePackageData<ArtifactReference> r) {
            materializeArtifact(root.resolve(r.v.getPackageName()), r.result.getRight());
            map(r.children);
            return ObjectUtils.NULL;
        }

        private void map(Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> children) {
            for(Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>> e : children.values()) {
                if(e.getKey() != NormalDependencyType.STRONG) {
                    continue;
                }
                transform(e.getValue());
            }
        }
    }

    public static void materializeStrongDependencyArtifacts(final Path root, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyArtifacts) {
        QbtUtils.mkdirs(root);
        new StrongDependencyMaterializer(root).map(dependencyArtifacts);
    }

    private static void materializeStrongArtifacts(final Path root, CvRecursivePackageData<ArtifactReference> artifactPile) {
        QbtUtils.mkdirs(root);
        new StrongDependencyMaterializer(root).map(artifactPile);
    }

    public static void materializeWeakArtifacts(Path root, Set<NormalDependencyType> types, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyArtifacts) {
        QbtUtils.mkdirs(root);
        for(Map.Entry<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> e : dependencyArtifacts.entrySet()) {
            if(!types.contains(e.getValue().getLeft())) {
                continue;
            }
            materializeRuntimeArtifacts(root.resolve(e.getKey()), e.getValue().getRight());
        }
    }

    private static void materializeBuildtimeArtifacts(Path root, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<ArtifactReference>>> dependencyArtifacts) {
        materializeStrongDependencyArtifacts(root.resolve("strong"), dependencyArtifacts);
        materializeWeakArtifacts(root.resolve("weak"), ImmutableSet.of(NormalDependencyType.BUILDTIME_WEAK, NormalDependencyType.RUNTIME_WEAK), dependencyArtifacts);
    }

    public static void materializeRuntimeArtifacts(Path root, CvRecursivePackageData<ArtifactReference> artifactPile) {
        materializeStrongArtifacts(root.resolve("strong"), artifactPile);
        materializeWeakArtifacts(root.resolve("weak"), ImmutableSet.of(NormalDependencyType.RUNTIME_WEAK), artifactPile.children);
    }

    public static <T> T runPackageCommand(String[] command, BuildData bd, Function<ProcessHelper, T> cb) {
        try(QbtTempDir tempDir = new QbtTempDir()) {
            Path inputsDir = tempDir.path;
            materializeBuildtimeArtifacts(inputsDir, bd.dependencyArtifacts);

            try(PackageDirectory packageDirectory = PackageDirectories.forBuildData(bd)) {
                Path packageDir = packageDirectory.getDir();
                ProcessHelper p = new ProcessHelper(packageDir, command);
                p = p.putEnv("INPUT_ARTIFACTS_DIR", inputsDir.toAbsolutePath().toString());
                p = p.putEnv("PACKAGE_DIR", packageDir.toAbsolutePath().toString());
                p = p.putEnv("PACKAGE_NAME", bd.v.getPackageName());
                p = p.putEnv("PACKAGE_CUMULATIVE_VERSION", bd.v.getDigest().getRawDigest().toString());
                p = p.stripEnv(new Predicate<Pair<String, String>>() {
                    @Override
                    public boolean apply(Pair<String, String> e) {
                        return e.getKey().startsWith("QBT_ENV_");
                    }
                });
                for(Map.Entry<String, String> e : bd.v.result.qbtEnv.entrySet()) {
                    p = p.putEnv("QBT_ENV_" + e.getKey(), e.getValue());
                }
                return cb.apply(p);
            }
        }
    }

    public static Pair<Result<ArtifactReference>, ArtifactReference> runBuild(ArtifactScope artifactScope, BuildData bd) {
        try(QbtTempDir tempDir = new QbtTempDir()) {
            final Path artifactsDir = tempDir.resolve("artifacts");
            QbtUtils.mkdirs(artifactsDir);
            final Path reportsDir = tempDir.resolve("reports");
            QbtUtils.mkdirs(reportsDir);

            PackageBuildType buildType = bd.metadata.get(PackageMetadataType.BUILD_TYPE);
            RuntimeException failure = null;
            try {
                switch(buildType) {
                    case NORMAL:
                        runNormalBuild(artifactsDir, reportsDir, bd);
                        break;

                    case COPY:
                        runCopyBuild(artifactsDir, reportsDir, bd);
                        break;

                    default:
                        throw new IllegalStateException("Unknown buildType: " + buildType);
                }
            }
            catch(RuntimeException e) {
                failure = e;
            }

            for(Path dir : new Path[] {artifactsDir, reportsDir}) {
                QbtUtils.writeLines(dir.resolve("qbt.versionDigest"), ImmutableList.of(bd.v.getDigest().getRawDigest().toString()));
                QbtUtils.writeLines(dir.resolve("qbt.versionTree"), bd.v.prettyTree());
            }

            Result<ArtifactReference> artifactsResult;
            if(failure != null) {
                artifactsResult = Result.newFailure(failure);
            }
            else {
                artifactsResult = Result.newSuccess(artifactScope.copyDirectory(artifactsDir));
            }
            ArtifactReference reportsResult = artifactScope.copyDirectory(reportsDir);
            return Pair.of(artifactsResult, reportsResult);
        }
    }

    private static void runNormalBuild(final Path artifactsDir, final Path reportsDir, final BuildData bd) {
        Maybe<String> prefix = bd.metadata.get(PackageMetadataType.PREFIX);
        if(!prefix.isPresent()) {
            return;
        }

        int exitCode = runPackageCommand(new String[] {"./qbt-make"}, bd, new Function<ProcessHelper, Integer>() {
            @Override
            public Integer apply(ProcessHelper p) {
                p = p.combineError();
                p = p.putEnv("OUTPUT_ARTIFACTS_DIR", artifactsDir.toString());
                p = p.putEnv("OUTPUT_REPORTS_DIR", reportsDir.toString());
                return p.completeLinesCallbackExitCode(new Function<String, Void>() {
                    @Override
                    public Void apply(String line) {
                        LOGGER.info("[" + bd.v.prettyDigest() + "] " + line);
                        return null;
                    }
                });
            }
        });
        if(exitCode != 0) {
            throw new RuntimeException("Build for " + bd.v.prettyDigest() + " returned non-zero: " + exitCode);
        }
    }

    private static void runCopyBuild(Path artifactsDir, Path reportsDir, final BuildData bd) {
        try(PackageDirectory packageDir = PackageDirectories.forBuildData(bd)) {
            // arggh, java file APIs so horrible, we tar/untar...
            try(QbtTempDir tempDir = new QbtTempDir()) {
                Path tarball = tempDir.resolve("package.tar.gz");
                TarballUtils.unexplodeTarball(packageDir.getDir(), tarball);
                TarballUtils.explodeTarball(artifactsDir, tarball);
            }
        }
    }
}
