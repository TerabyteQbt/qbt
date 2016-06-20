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
package qbt.config;

import java.nio.file.Path;
import qbt.VcsVersionDigest;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;

public interface LocalPinsRepo {
    PinnedRepoAccessor findPin(RepoTip repo, VcsVersionDigest version);
    void addPin(RepoTip repo, Path dir, VcsVersionDigest version);
    void fetchPins(RepoTip repo, RawRemote remote);
    default PinnedRepoAccessor requirePin(RepoTip repo, VcsVersionDigest version, String message) {
        PinnedRepoAccessor r = findPin(repo, version);
        if(r == null) {
            throw new IllegalArgumentException(message);
        }
        return r;
    }
    default PinnedRepoAccessor requirePin(RepoTip repo, VcsVersionDigest version) {
        return requirePin(repo, version, "Could not find local pin for " + repo + " at " + version);
    }
}
