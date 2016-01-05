package qbt.build;

import com.google.common.base.Function;
import misc1.commons.Result;
import misc1.commons.concurrent.ctree.ComputationTree;
import misc1.commons.options.OptionsResults;
import misc1.commons.resources.FreeScope;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.VcsTreeDigest;
import qbt.artifactcacher.Architecture;
import qbt.artifactcacher.ArtifactCacher;
import qbt.artifactcacher.ArtifactReference;
import qbt.manifest.current.PackageMetadata;

public final class PackageMapperHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageMapperHelper.class);

    private PackageMapperHelper() {
        // hidden
    }

    public interface PackageMapperHelperCallback<T> {
        public ComputationTree<T> run(PackageMapperHelperCallbackCallback cb);
    }

    public interface PackageMapperHelperCallbackCallback {
        ArtifactReference runBuild(BuildData bd);
        Pair<Result<ArtifactReference>, ArtifactReference> runBuildFailable(BuildData bd);
    }

    public static <O, T> T run(final ArtifactCacher artifactCacher, OptionsResults<? extends O> options, PackageMapperHelperOptionsDelegate<? super O> packageMapperHelperOption, PackageMapperHelperCallback<T> cb) {
        artifactCacher.cleanup();
        final Architecture arch = Architecture.fromArg(options.get(packageMapperHelperOption.arch));
        final boolean noBuilds = options.get(packageMapperHelperOption.noBuilds);
        try(FreeScope scope = new FreeScope()) {
            ComputationTree<T> computationTree = cb.run(new PackageMapperHelperCallbackCallback() {
                private void checkTree(BuildData bd, String suffix) {
                    // This is sort of a crummy way to do this.
                    // Additionally, this is only checking overrides
                    // since non-overrides have a fixed effective tree.
                    VcsTreeDigest currentTree = bd.commonRepoAccessor.getEffectiveTree(bd.metadata.get(PackageMetadata.PREFIX));
                    if(!bd.v.getEffectiveTree().equals(currentTree)) {
                        throw new IllegalStateException("The effective tree for " + bd.v.getPackageName() + " has changed from " + bd.v.getEffectiveTree().getRawDigest() + " to " + currentTree.getRawDigest() + suffix + "!");
                    }
                }

                @Override
                public ArtifactReference runBuild(BuildData bd) {
                    return runBuildFailable(bd).getLeft().getCommute();
                }

                @Override
                public Pair<Result<ArtifactReference>, ArtifactReference> runBuildFailable(final BuildData bd) {
                    Pair<Architecture, ArtifactReference> artifactPair = artifactCacher.get(scope, bd.v.getDigest(), Architecture.independent());
                    String cacheDesc = "missed";
                    if(artifactPair != null) {
                        cacheDesc = "hit (INDEPENDENT)";
                    }
                    else {
                        artifactPair = artifactCacher.get(scope, bd.v.getDigest(), arch);
                        if(artifactPair != null) {
                            cacheDesc = "hit (" + arch + ")";
                        }
                    }
                    LOGGER.debug("Cache check " + bd.v.getPackageName() + " at " + bd.v.prettyDigest() + ", " + cacheDesc);
                    if(artifactPair != null) {
                        return Pair.of(Result.newSuccess(artifactPair.getRight()), null);
                    }
                    else {
                        String buildDesc = bd.v.prettyDigest();
                        if(noBuilds) {
                            return Pair.of(Result.<ArtifactReference>newFailure(new RuntimeException("Would have built " + buildDesc + " but builds were forbidden.")), null);
                        }

                        checkTree(bd, " before the build");

                        LOGGER.info("Actually building " + buildDesc + "...");
                        Pair<Result<ArtifactReference>, ArtifactReference> result = BuildUtils.runBuild(scope, bd);
                        Result<ArtifactReference> artifactResult = result.getLeft();
                        artifactResult = artifactResult.transform(new Function<ArtifactReference, ArtifactReference>() {
                            @Override
                            public ArtifactReference apply(ArtifactReference input) {
                                return artifactCacher.intercept(scope, bd.v.getDigest(), Pair.of(bd.metadata.get(PackageMetadata.ARCH_INDEPENDENT) ? Architecture.independent() : arch, input)).getRight();
                            }
                        });

                        checkTree(bd, " after the build");

                        return result;
                    }
                }
            });
            return packageMapperHelperOption.parallelism.getResult(options, false).runComputationTree(computationTree);
        }
    }
}
