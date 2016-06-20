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
package qbt.vcs.git;

import java.nio.file.Path;
import qbt.VcsVersionDigest;
import qbt.vcs.LocalVcs;
import qbt.vcs.simple.SimpleRawRemoteVcs;

public final class GitRawRemoteVcs extends SimpleRawRemoteVcs {
    @Override
    public LocalVcs getLocalVcs() {
        return new GitLocalVcs();
    }

    @Override
    public void fetchPins(Path dir, String remote) {
        GitUtils.fetchPins(dir, remote);
    }

    @Override
    public void addPinToRemote(Path dir, String remote, VcsVersionDigest commit) {
        GitUtils.addPinToRemote(dir, remote, commit);
    }

    @Override
    public boolean remoteExists(String remote) {
        return GitUtils.remoteExists(remote);
    }
}
