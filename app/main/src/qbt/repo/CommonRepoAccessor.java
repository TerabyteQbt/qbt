package qbt.repo;

import misc1.commons.Maybe;
import qbt.PackageDirectory;
import qbt.VcsTreeDigest;

public interface CommonRepoAccessor {
    public PackageDirectory makePackageDirectory(String prefix);
    public VcsTreeDigest getEffectiveTree(Maybe<String> prefix);
    public VcsTreeDigest getSubtree(VcsTreeDigest tree, String prefix);
    public boolean isOverride();
}
