package qbt.pins;

import qbt.VcsVersionDigest;
import qbt.config.LocalPinsRepo;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;

public abstract class AbstractLocalPinsRepo implements LocalPinsRepo {
    @Override
    public PinnedRepoAccessor requirePin(RepoTip repo, VcsVersionDigest version) {
        PinnedRepoAccessor r = findPin(repo, version);
        if(r == null) {
            throw new IllegalArgumentException("Could not find local pin for " + repo + " at " + version);
        }
        return r;
    }
}
