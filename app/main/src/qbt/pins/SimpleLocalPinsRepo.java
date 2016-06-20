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
import org.apache.commons.lang3.ObjectUtils;
import qbt.QbtUtils;
import qbt.VcsVersionDigest;
import qbt.config.LocalPinsRepo;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;
import qbt.vcs.RawRemoteVcs;

public final class SimpleLocalPinsRepo implements LocalPinsRepo {
    private final RawRemoteVcs vcs;
    private final Path root;

    public SimpleLocalPinsRepo(RawRemoteVcs vcs, Path root) {
        this.vcs = vcs;
        this.root = root;
    }

    private Path materializeCache(RepoTip repo) {
        Path cache = root.resolve(repo.name);

        QbtUtils.semiAtomicDirCache(cache, "", (cacheTemp) -> {
            vcs.getLocalVcs().createCacheRepo(cacheTemp);
            return ObjectUtils.NULL;
        });

        return cache;
    }

    @Override
    public PinnedRepoAccessor findPin(RepoTip repo, VcsVersionDigest version) {
        Path cache = materializeCache(repo);

        if(!vcs.getLocalVcs().getRepository(cache).commitExists(version)) {
            return null;
        }

        return new PinnedRepoAccessor(vcs, cache, version);
    }

    @Override
    public void addPin(RepoTip repo, Path dir, VcsVersionDigest version) {
        Path cache = materializeCache(repo);
        vcs.addPinToRemote(dir, cache.toAbsolutePath().toString(), version);
    }

    @Override
    public void fetchPins(RepoTip repo, RawRemote remote) {
        Path cache = materializeCache(repo);

        RawRemoteVcs remoteVcs = remote.getRawRemoteVcs();
        if(!remoteVcs.equals(vcs)) {
            throw new IllegalStateException("VCS mismatch between pin " + vcs + " and remote " + remoteVcs);
        }

        vcs.fetchPins(cache, remote.getRemoteString());
    }
}
