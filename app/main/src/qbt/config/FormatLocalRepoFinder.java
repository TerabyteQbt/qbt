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
import java.nio.file.Paths;
import qbt.tip.RepoTip;
import qbt.vcs.LocalVcs;

public class FormatLocalRepoFinder extends LocalRepoFinder {
    private final String format;

    public FormatLocalRepoFinder(LocalVcs vcs, String format) {
        super(vcs);
        this.format = format;
    }

    protected Path directory(RepoTip repo) {
        return Paths.get(format.replace("%r", repo.name).replace("%t", repo.tip));
    }
}
