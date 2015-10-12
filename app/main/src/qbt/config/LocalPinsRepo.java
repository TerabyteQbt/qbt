package qbt.config;

import qbt.PackageTip;
import qbt.VcsVersionDigest;
import qbt.repo.PinnedRepoAccessor;
import qbt.vcs.RawRemote;

public interface LocalPinsRepo {
    public PinnedRepoAccessor findPin(PackageTip repo, VcsVersionDigest version);
    public PinnedRepoAccessor requirePin(PackageTip repo, VcsVersionDigest version);
    public void fetchPins(PackageTip repo, RawRemote remote);
}
