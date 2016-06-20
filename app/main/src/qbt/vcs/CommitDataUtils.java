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
package qbt.vcs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import qbt.VcsVersionDigest;

public final class CommitDataUtils {
    private CommitDataUtils() {
        // no
    }

    public static Iterable<Pair<VcsVersionDigest, CommitData>> revWalkFlatten(final Map<VcsVersionDigest, CommitData> revWalk, Iterable<VcsVersionDigest> commits) {
        final ImmutableList.Builder<Pair<VcsVersionDigest, CommitData>> b = ImmutableList.builder();
        final Set<VcsVersionDigest> already = Sets.newHashSet();
        class Helper {
            public void build(VcsVersionDigest commit) {
                if(!already.add(commit)) {
                    return;
                }
                CommitData commitData = revWalk.get(commit);
                if(commitData == null) {
                    return;
                }
                for(VcsVersionDigest parent : commitData.get(CommitData.PARENTS)) {
                    build(parent);
                }
                b.add(Pair.of(commit, commitData));
            }
        }
        Helper h = new Helper();
        for(VcsVersionDigest commit : commits) {
            h.build(commit);
        }
        return b.build().reverse();
    }
}
