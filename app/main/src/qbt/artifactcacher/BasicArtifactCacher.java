package qbt.artifactcacher;

import misc1.commons.resources.FreeScope;
import org.apache.commons.lang3.tuple.Pair;
import qbt.recursive.cv.CumulativeVersionDigest;

public interface BasicArtifactCacher extends ArtifactCacher {
    @Override
    default Pair<Architecture, ArtifactReference> intercept(FreeScope scope, CumulativeVersionDigest key, Pair<Architecture, ArtifactReference> p) {
        put(p.getLeft(), key, p.getRight());
        return p;
    }

    default void put(Architecture arch, CumulativeVersionDigest key, ArtifactReference artifact) {
    }
}
