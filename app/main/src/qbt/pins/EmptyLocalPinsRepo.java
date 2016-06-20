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
package qbt.pins;

import java.nio.file.Path;
import qbt.VcsVersionDigest;
import qbt.config.LocalPinsRepo;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;

public final class EmptyLocalPinsRepo implements LocalPinsRepo {
    @Override
    public void addPin(RepoTip repo, Path dir, VcsVersionDigest version) {
    }

    @Override
    public PinnedRepoAccessor findPin(RepoTip repo, VcsVersionDigest version) {
        return null;
    }

    @Override
    public void fetchPins(RepoTip repo, RawRemote remote) {
    }
}
