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
package qbt.remote;

import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;
import qbt.vcs.RawRemoteVcs;

public final class FormatQbtRemote implements QbtRemote {
    private final RawRemoteVcs vcs;
    private final String format;

    public FormatQbtRemote(RawRemoteVcs vcs, String format) {
        this.vcs = vcs;
        this.format = format;
    }

    private String formatRemote(RepoTip repo) {
        return format.replace("%r", repo.name).replace("%t", repo.tip);
    }

    @Override
    public RawRemote findRemote(RepoTip repo, boolean autoVivify) {
        String remote = formatRemote(repo);
        RawRemote rawRemote = new RawRemote(remote, vcs);

        if(vcs.remoteExists(remote)) {
            return rawRemote;
        }
        if(!autoVivify) {
            // doesn't exist and we weren't asked to create it
            return null;
        }
        // If autoVivify asked for, which FormatQbtRemote doesn't support, return the RawRemote and let the fireworks happen elsewhere.
        return rawRemote;
    }
}
