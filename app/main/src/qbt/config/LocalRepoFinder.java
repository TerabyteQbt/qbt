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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.QbtUtils;
import qbt.repo.LocalRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.LocalVcs;

public abstract class LocalRepoFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRepoFinder.class);

    private final LocalVcs vcs;

    public LocalRepoFinder(LocalVcs vcs) {
        this.vcs = vcs;
    }

    public LocalRepoAccessor findLocalRepo(RepoTip repo) {
        final Path repoDir = directory(repo);
        if(!vcs.isRepo(repoDir)) {
            LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " missed");
            return null;
        }
        LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " hit");
        return new LocalRepoAccessor(vcs, repoDir);
    }

    public LocalRepoAccessor createLocalRepo(RepoTip repo) {
        final Path repoDir = directory(repo);
        if(repoDir.toFile().exists()) {
            throw new IllegalArgumentException("Local repo for " + repo + " already exists in " + repoDir);
        }
        QbtUtils.mkdirs(repoDir);
        vcs.createWorkingRepo(repoDir);
        return new LocalRepoAccessor(vcs, repoDir);
    }

    protected abstract Path directory(RepoTip repo);
}
