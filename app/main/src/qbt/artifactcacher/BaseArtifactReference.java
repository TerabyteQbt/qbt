package qbt.artifactcacher;

import java.nio.file.Path;
import misc1.commons.Maybe;
import misc1.commons.resources.FreeScope;

public interface BaseArtifactReference {
    public void materializeDirectory(Maybe<FreeScope> scope, Path destination);
    public void materializeTarball(Path destination);
}
