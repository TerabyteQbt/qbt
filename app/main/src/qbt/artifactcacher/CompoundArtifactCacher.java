//   Copyright 2016 Keith Amling
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
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
            touch(depth + 1, key, arch);
            return delegateHit;
        }
        Pair<Architecture, ArtifactReference> tailHit = get(depth + 1, scope, key, arch);
        if(tailHit == null) {
            return null;
        }
        return delegate.intercept(scope, key, tailHit);
    }

    @Override
    public void touch(CumulativeVersionDigest key, Architecture arch) {
        touch(0, key, arch);
    }

    private void touch(int depth, CumulativeVersionDigest key, Architecture arch) {
        if(depth >= delegates.size()) {
            return;
        }
        ArtifactCacher delegate = delegates.get(depth);
        delegate.touch(key, arch);
        touch(depth + 1, key, arch);
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
