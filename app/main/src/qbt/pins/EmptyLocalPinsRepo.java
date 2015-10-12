package qbt.pins;

import qbt.PackageTip;
import qbt.VcsVersionDigest;
import qbt.repo.PinnedRepoAccessor;
import qbt.vcs.RawRemote;

public final class EmptyLocalPinsRepo extends AbstractLocalPinsRepo {
    @Override
    public PinnedRepoAccessor findPin(PackageTip repo, VcsVersionDigest version) {
        return null;
    }

    @Override
    public void fetchPins(PackageTip repo, RawRemote remote) {
    }
}
