package qbt.pins;

import qbt.VcsVersionDigest;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;

public final class EmptyLocalPinsRepo extends AbstractLocalPinsRepo {
    @Override
    public PinnedRepoAccessor findPin(RepoTip repo, VcsVersionDigest version) {
        return null;
    }

    @Override
    public void fetchPins(RepoTip repo, RawRemote remote) {
    }
}
