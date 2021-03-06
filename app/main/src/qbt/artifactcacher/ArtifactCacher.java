package qbt.artifactcacher;

import misc1.commons.resources.FreeScope;
import org.apache.commons.lang3.tuple.Pair;
import qbt.recursive.cv.CumulativeVersionDigest;

public interface ArtifactCacher {
    default Pair<Architecture, ArtifactReference> get(FreeScope scope, CumulativeVersionDigest key, Architecture arch) {
        return null;
    }
    default void touch(CumulativeVersionDigest key, Architecture arch) {
    }
    default Pair<Architecture, ArtifactReference> intercept(FreeScope scope, CumulativeVersionDigest key, Pair<Architecture, ArtifactReference> p) {
        return p;
    }
    default void cleanup() {
    }
}
