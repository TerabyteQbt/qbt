package qbt.artifactcacher;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import qbt.recursive.cv.CumulativeVersionDigest;

public class CompoundArtifactCacher implements ArtifactCacher {
    private final List<ArtifactCacher> delegates;

    public CompoundArtifactCacher(List<ArtifactCacher> delegates) {
        this.delegates = ImmutableList.copyOf(delegates);
    }

    @Override
    public Pair<Architecture, ArtifactReference> get(ArtifactScope artifactScope, Architecture arch, CumulativeVersionDigest key) {
        return get(0, artifactScope, arch, key);
    }

    private Pair<Architecture, ArtifactReference> get(int depth, ArtifactScope artifactScope, Architecture arch, CumulativeVersionDigest key) {
        if(depth >= delegates.size()) {
            return null;
        }
        ArtifactCacher delegate = delegates.get(depth);
        Pair<Architecture, ArtifactReference> delegateHit = delegate.get(artifactScope, arch, key);
        if(delegateHit != null) {
            return delegateHit;
        }
        Pair<Architecture, ArtifactReference> tailHit = get(depth + 1, artifactScope, arch, key);
        if(tailHit == null) {
            return null;
        }
        return delegate.intercept(key, tailHit);
    }

    @Override
    public Pair<Architecture, ArtifactReference> intercept(CumulativeVersionDigest key, Pair<Architecture, ArtifactReference> p) {
        return intercept(0, key, p);
    }

    private Pair<Architecture, ArtifactReference> intercept(int depth, CumulativeVersionDigest key, Pair<Architecture, ArtifactReference> p) {
        if(depth >= delegates.size()) {
            return p;
        }
        ArtifactCacher delegate = delegates.get(depth);
        return delegate.intercept(key, intercept(depth + 1, key, p));
    }

    @Override
    public void cleanup() {
        for(ArtifactCacher delegate : delegates) {
            delegate.cleanup();
        }
    };
}
