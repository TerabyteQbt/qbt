package qbt.config;

import qbt.VcsVersionDigest;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;

public interface LocalPinsRepo {
    public PinnedRepoAccessor findPin(RepoTip repo, VcsVersionDigest version);
    public PinnedRepoAccessor requirePin(RepoTip repo, VcsVersionDigest version);
    public void fetchPins(RepoTip repo, RawRemote remote);
}
