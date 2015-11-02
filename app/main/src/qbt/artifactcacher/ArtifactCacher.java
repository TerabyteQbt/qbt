package qbt.artifactcacher;

import misc1.commons.resources.FreeScope;
import org.apache.commons.lang3.tuple.Pair;
import qbt.recursive.cv.CumulativeVersionDigest;

public interface ArtifactCacher {
    Pair<Architecture, ArtifactReference> get(FreeScope scope, CumulativeVersionDigest key, Architecture arch);
    Pair<Architecture, ArtifactReference> intercept(FreeScope scope, CumulativeVersionDigest key, Pair<Architecture, ArtifactReference> p);
    void cleanup();
}
