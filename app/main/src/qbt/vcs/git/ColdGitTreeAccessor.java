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
import java.util.Collection;
import misc1.commons.Either;
import qbt.VcsTreeDigest;
import qbt.vcs.TreeAccessor;

public class ColdGitTreeAccessor implements TreeAccessor {
    private final Path dir;
    private final VcsTreeDigest tree;

    private volatile TreeAccessor hotDelegate = null;

    public ColdGitTreeAccessor(Path dir, VcsTreeDigest tree) {
        this.dir = dir;
        this.tree = tree;
    }

    private TreeAccessor getDelegate() {
        TreeAccessor hotDelegateLocal = hotDelegate;
        if(hotDelegateLocal == null) {
            hotDelegateLocal = hotDelegate = new HotGitTreeAccessor(dir, tree);
        }
        return hotDelegateLocal;
    }

    @Override
    public TreeAccessor replace(String path, byte[] contents) {
        return getDelegate().replace(path, contents);
    }

    @Override
    public Either<TreeAccessor, byte[]> get(String path) {
        return getDelegate().get(path);
    }

    @Override
    public TreeAccessor remove(String path) {
        return getDelegate().remove(path);
    }

    @Override
    public VcsTreeDigest getDigest() {
        return getDelegate().getDigest();
    }

    @Override
    public boolean isEmpty() {
        return getDelegate().isEmpty();
    }

    @Override
    public Collection<String> getEntryNames() {
        return getDelegate().getEntryNames();
    }
}
