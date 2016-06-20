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

import java.nio.file.Files;
import java.nio.file.Path;
import qbt.QbtUtils;
import qbt.VcsTreeDigest;
import qbt.vcs.Repository;
import qbt.vcs.simple.SimpleLocalVcs;

public final class GitLocalVcs extends SimpleLocalVcs {
    @Override
    public String getName() {
        return "git";
    }

    @Override
    public boolean isRepo(Path dir) {
        return Files.isDirectory(dir.resolve(".git"));
    }

    private void checkEmpty(Path dir) {
        if(Files.isDirectory(dir) && QbtUtils.listChildren(dir).size() > 0) {
            throw new RuntimeException("Path already exists: " + dir);
        }
    }


    @Override
    public Repository getRepository(Path dir) {
        return new GitRepository(dir);
    }

    @Override
    public Repository createWorkingRepo(Path dir) {
        checkEmpty(dir);
        GitUtils.createWorkingRepo(dir);
        return new GitRepository(dir);
    }

    @Override
    public Repository createCacheRepo(Path dir) {
        checkEmpty(dir);
        GitUtils.createCacheRepo(dir);
        return new GitRepository(dir);
    }

    @Override
    public VcsTreeDigest emptyTree() {
        return GitUtils.EMPTY_TREE;
    }

}
