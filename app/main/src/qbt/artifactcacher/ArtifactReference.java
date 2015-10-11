package qbt.artifactcacher;

import java.nio.file.Path;

public interface ArtifactReference {
    void materializeDirectory(Path destination);
    void materializeTarball(Path destination);
    ArtifactReference copyInto(ArtifactScope artifactScope);
}
