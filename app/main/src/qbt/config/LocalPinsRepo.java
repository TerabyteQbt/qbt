package qbt.config;

import java.nio.file.Path;
import qbt.VcsVersionDigest;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;

public interface LocalPinsRepo {
    PinnedRepoAccessor findPin(RepoTip repo, VcsVersionDigest version);
    void addPin(RepoTip repo, Path dir, VcsVersionDigest version);
    void fetchPins(RepoTip repo, RawRemote remote);
    default PinnedRepoAccessor requirePin(RepoTip repo, VcsVersionDigest version, String message) {
        PinnedRepoAccessor r = findPin(repo, version);
        if(r == null) {
            throw new IllegalArgumentException(message);
        }
        return r;
    }
    default PinnedRepoAccessor requirePin(RepoTip repo, VcsVersionDigest version) {
        return requirePin(repo, version, "Could not find local pin for " + repo + " at " + version);
    }
}
