package qbt.pins;

import com.google.common.base.Function;
import java.nio.file.Path;
import org.apache.commons.lang3.ObjectUtils;
import qbt.PackageTip;
import qbt.QbtUtils;
import qbt.VcsVersionDigest;
import qbt.repo.PinnedRepoAccessor;
import qbt.vcs.RawRemote;
import qbt.vcs.RawRemoteVcs;

public final class SimpleLocalPinsRepo extends AbstractLocalPinsRepo {
    private final RawRemoteVcs vcs;
    private final Path root;

    public SimpleLocalPinsRepo(RawRemoteVcs vcs, Path root) {
        this.vcs = vcs;
        this.root = root;
    }

    private Path materializeCache(PackageTip repo) {
        Path cache = root.resolve(repo.pkg);

        QbtUtils.semiAtomicDirCache(cache, "", new Function<Path, ObjectUtils.Null>() {
            @Override
            public ObjectUtils.Null apply(Path cacheTemp) {
                vcs.getLocalVcs().createCacheRepo(cacheTemp);
                return ObjectUtils.NULL;
            }
        });

        return cache;
    }

    @Override
    public PinnedRepoAccessor findPin(PackageTip repo, VcsVersionDigest version) {
        Path cache = materializeCache(repo);

        if(!vcs.getLocalVcs().getRepository(cache).commitExists(version)) {
            return null;
        }

        return new PinnedRepoAccessor(vcs, cache, version);
    }

    @Override
    public void fetchPins(PackageTip repo, RawRemote remote) {
        Path cache = materializeCache(repo);

        RawRemoteVcs remoteVcs = remote.getRawRemoteVcs();
        if(!remoteVcs.equals(vcs)) {
            throw new IllegalStateException("VCS mismatch between pin " + vcs + " and remote " + remoteVcs);
        }

        vcs.fetchPins(cache, remote.getRemoteString());
    }
}
