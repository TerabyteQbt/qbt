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
package qbt.repo;

import java.nio.file.Path;
import misc1.commons.Maybe;
import misc1.commons.concurrent.treelock.ArrayTreeLock;
import misc1.commons.concurrent.treelock.ArrayTreeLockPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.PackageDirectory;
import qbt.VcsTreeDigest;
import qbt.repo.CommonRepoAccessor;
import qbt.vcs.LocalVcs;

public class LocalRepoAccessor implements CommonRepoAccessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRepoAccessor.class);

    public final LocalVcs vcs;
    public final Path dir;

    public LocalRepoAccessor(LocalVcs vcs, Path dir) {
        this.vcs = vcs;
        this.dir = dir;
    }

    private Path packageDir(String prefix) {
        return prefix.isEmpty() ? dir : dir.resolve(prefix);
    }

    @Override
    public PackageDirectory makePackageDirectory(final String prefix) {
        lock(prefix);
        return new PackageDirectory() {
            @Override
            public Path getDir() {
                return packageDir(prefix);
            }

            @Override
            public void close() {
                unlock(prefix);
            }
        };
    }

    @Override
    public VcsTreeDigest getEffectiveTree(Maybe<String> prefix) {
        if(prefix.isPresent()) {
            return vcs.getRepository(dir).getEffectiveTree(prefix.get(null));
        }
        else {
            return vcs.emptyTree();
        }
    }

    @Override
    public VcsTreeDigest getSubtree(VcsTreeDigest tree, String subpath) {
        return vcs.getRepository(dir).getSubtree(tree, subpath);
    }

    @Override
    public boolean isOverride() {
        return true;
    }

    private final ArrayTreeLock<String> inUse = new ArrayTreeLock<String>();

    private void lock(String prefix) {
        LOGGER.debug("Local repo locking " + dir.resolve(prefix));
        inUse.lock(ArrayTreeLockPath.split(prefix, '/'));
    }

    private void unlock(String prefix) {
        LOGGER.debug("Local repo unlocking " + dir.resolve(prefix));
        inUse.unlock(ArrayTreeLockPath.split(prefix, '/'));
    }
}
