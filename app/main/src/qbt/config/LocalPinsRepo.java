package qbt.config;

import qbt.VcsVersionDigest;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;

public interface LocalPinsRepo {
    PinnedRepoAccessor findPin(RepoTip repo, VcsVersionDigest version);
    void fetchPins(RepoTip repo, RawRemote remote);
    default PinnedRepoAccessor requirePin(RepoTip repo, VcsVersionDigest version) {
        PinnedRepoAccessor r = findPin(repo, version);
        if(r == null) {
            throw new IllegalArgumentException("Could not find local pin for " + repo + " at " + version);
        }
        return r;
    }
}
