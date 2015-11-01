package qbt.artifactcacher;

import com.google.common.base.Function;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import misc1.commons.ExceptionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import qbt.QbtUtils;
import qbt.recursive.cv.CumulativeVersionDigest;
import qbt.utils.TarballUtils;

public class SymlinkingMadnessArtifactCacher implements ArtifactCacher {
    private final Path writeRoot;
    private final Path readRoot;

    public SymlinkingMadnessArtifactCacher(Path root) {
        this(root, root);
    }

    public SymlinkingMadnessArtifactCacher(Path writeRoot, Path readRoot) {
        this.writeRoot = writeRoot;
        this.readRoot = readRoot;
    }

    @Override
    public Pair<Architecture, ArtifactReference> get(ArtifactScope artifactScope, Architecture arch, CumulativeVersionDigest key) {
        Path dir = readRoot.resolve(key.getRawDigest().toString());
        if(Files.isDirectory(dir)) {
            return Pair.of(Architecture.unknown(), artifactReference(dir));
        }
        return null;
    }

    @Override
    public Pair<Architecture, ArtifactReference> intercept(CumulativeVersionDigest key, final Pair<Architecture, ArtifactReference> p) {
        final Path writeDir = writeRoot.resolve(key.getRawDigest().toString());
        final Path readDir = readRoot.resolve(key.getRawDigest().toString());

        boolean copied = QbtUtils.semiAtomicDirCache(writeDir, "", new Function<Path, ObjectUtils.Null>() {
            @Override
            public ObjectUtils.Null apply(Path tempDir) {
                p.getRight().materializeDirectory(tempDir);
                return ObjectUtils.NULL;
            }
        });

        if(!copied) {
            // hit on disk, do not honor alleged architecture
            return Pair.of(Architecture.unknown(), artifactReference(readDir));
        }

        return Pair.of(p.getLeft(), artifactReference(readDir));
    }

    private static ArtifactReference artifactReference(final Path dir) {
        return new ArtifactReference() {
            @Override
            public void materializeTarball(Path destination) {
                // suxco
                TarballUtils.unexplodeTarball(dir, destination);
            }

            @Override
            public void materializeDirectory(Path destination) {
                // also slightly suxco
                for(Path p : QbtUtils.listChildren(dir)) {
                    try {
                        Files.createSymbolicLink(destination.resolve(p.getFileName()), p);
                    }
                    catch(IOException e) {
                        throw ExceptionUtils.commute(e);
                    }
                }
            }

            @Override
            public ArtifactReference copyInto(ArtifactScope artifactScope) {
                return this;
            }
        };
    }

    @Override
    public void cleanup() {
        // nope, unfortunately
    }
}
