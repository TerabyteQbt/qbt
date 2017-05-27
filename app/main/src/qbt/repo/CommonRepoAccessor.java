package qbt.repo;

import qbt.PackageDirectory;
import qbt.VcsTreeDigest;

public interface CommonRepoAccessor {
    public PackageDirectory makePackageDirectory(String prefix);
    public VcsTreeDigest getEffectiveTree(String prefix);
    public VcsTreeDigest getSubtree(VcsTreeDigest tree, String prefix);
    public boolean isOverride();
}
