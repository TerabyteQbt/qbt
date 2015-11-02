package qbt.artifactcacher;

import java.nio.file.Path;
import misc1.commons.Maybe;
import misc1.commons.resources.FreeScope;

public interface ArtifactReference {
    void materializeDirectory(Maybe<FreeScope> scope, Path destination);
    void materializeTarball(Path destination);
    ArtifactReference copyInto(ArtifactScope artifactScope);
}
