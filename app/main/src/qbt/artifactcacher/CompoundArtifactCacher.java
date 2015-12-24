package qbt.artifactcacher;

import com.google.common.collect.ImmutableList;
import java.util.List;
import misc1.commons.resources.FreeScope;
import org.apache.commons.lang3.tuple.Pair;
import qbt.recursive.cv.CumulativeVersionDigest;

public class CompoundArtifactCacher implements ArtifactCacher {
    private final List<ArtifactCacher> delegates;

    public CompoundArtifactCacher(List<ArtifactCacher> delegates) {
        this.delegates = ImmutableList.copyOf(delegates);
    }

    @Override
    public Pair<Architecture, ArtifactReference> get(FreeScope scope, CumulativeVersionDigest key, Architecture arch) {
        return get(0, scope, key, arch);
    }

    private Pair<Architecture, ArtifactReference> get(int depth, FreeScope scope, CumulativeVersionDigest key, Architecture arch) {
        if(depth >= delegates.size()) {
            return null;
        }
        ArtifactCacher delegate = delegates.get(depth);
        Pair<Architecture, ArtifactReference> delegateHit = delegate.get(scope, key, arch);
        if(delegateHit != null) {
            return delegateHit;
        }
        Pair<Architecture, ArtifactReference> tailHit = get(depth + 1, scope, key, arch);
        if(tailHit == null) {
            return null;
        }
        return delegate.intercept(scope, key, tailHit);
    }

    @Override
    public Pair<Architecture, ArtifactReference> intercept(FreeScope scope, CumulativeVersionDigest key, Pair<Architecture, ArtifactReference> p) {
        return intercept(0, scope, key, p);
    }

    private Pair<Architecture, ArtifactReference> intercept(int depth, FreeScope scope, CumulativeVersionDigest key, Pair<Architecture, ArtifactReference> p) {
        if(depth >= delegates.size()) {
            return p;
        }
        ArtifactCacher delegate = delegates.get(depth);
        return delegate.intercept(scope, key, intercept(depth + 1, scope, key, p));
    }

    @Override
    public void cleanup() {
        for(ArtifactCacher delegate : delegates) {
            delegate.cleanup();
        }
    }
}
