package qbt.vcs;

import misc1.commons.Either;
import qbt.VcsTreeDigest;

public interface TreeAccessor {
    public TreeAccessor replace(String path, byte[] contents);
    public Either<TreeAccessor, byte[]> get(String path);
    public TreeAccessor remove(String path);
    public VcsTreeDigest getDigest();
    public boolean isEmpty();
}
