package qbt.pins;

import java.nio.file.Path;
import misc1.commons.Maybe;
import qbt.PackageDirectory;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.config.LocalPinsRepo;
import qbt.repo.PinnedRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.LocalVcs;
import qbt.vcs.RawRemote;

/*
 * This is used by the QBT Integration tests
 */
public final class EmptyLocalPinsRepo implements LocalPinsRepo {
    @Override
    public PinnedRepoAccessor findPin(RepoTip repo, VcsVersionDigest version) {
        return new PinnedRepoAccessor() {

            @Override
            public PackageDirectory makePackageDirectory(String prefix) {
                return null;
            }

            @Override
            public VcsTreeDigest getEffectiveTree(Maybe<String> prefix) {
                return null;
            }

            @Override
            public VcsTreeDigest getSubtree(VcsTreeDigest tree, String subpath) {
                return null;
            }

            @Override
            public boolean isOverride() {
                return false;
            }

            @Override
            public void findCommit(Path dir) {
            }

            @Override
            public LocalVcs getLocalVcs() {
                return null;
            }

            @Override
            public void addPin(Path dir, VcsVersionDigest version) {
            }

            @Override
            public VcsTreeDigest getSubtree(String prefix) {
                return null;
            }

            @Override
            public void pushToRemote(RawRemote remote) {
            }

            @Override
            public boolean versionExists() {
                return false;
            }

        };
    }

    @Override
    public void fetchPins(RepoTip repo, RawRemote remote) {
    }
}
