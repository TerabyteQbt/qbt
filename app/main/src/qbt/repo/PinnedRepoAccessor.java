package qbt.repo;

import java.nio.file.Path;
import misc1.commons.Maybe;
import qbt.PackageDirectory;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.vcs.LocalVcs;
import qbt.vcs.RawRemote;

public interface PinnedRepoAccessor extends CommonRepoAccessor {

    PackageDirectory makePackageDirectory(String prefix);
    VcsTreeDigest getEffectiveTree(Maybe<String> prefix);
    VcsTreeDigest getSubtree(VcsTreeDigest tree, String subpath);
    boolean isOverride();
    void findCommit(Path dir);
    LocalVcs getLocalVcs();
    void addPin(Path dir, VcsVersionDigest version);
    VcsTreeDigest getSubtree(String prefix);
    void pushToRemote(RawRemote remote);
    boolean versionExists();
}

