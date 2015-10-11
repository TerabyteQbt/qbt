package qbt.artifactcacher;

import org.apache.commons.lang3.tuple.Pair;
import qbt.recursive.cv.CumulativeVersionDigest;

public interface ArtifactCacher {
    Pair<Architecture, ArtifactReference> get(ArtifactScope artifactScope, Architecture arch, CumulativeVersionDigest key);
    Pair<Architecture, ArtifactReference> intercept(CumulativeVersionDigest key, Pair<Architecture, ArtifactReference> p);
    void cleanup();
}
