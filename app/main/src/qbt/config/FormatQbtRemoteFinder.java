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

import qbt.remote.FormatQbtRemote;
import qbt.remote.QbtRemote;
import qbt.vcs.RawRemoteVcs;

public class FormatQbtRemoteFinder implements QbtRemoteFinder {
    private final RawRemoteVcs vcs;

    public FormatQbtRemoteFinder(RawRemoteVcs vcs) {
        this.vcs = vcs;
    }

    @Override
    public QbtRemote findQbtRemote(String remote) {
        return new FormatQbtRemote(vcs, remote);
    }
}
