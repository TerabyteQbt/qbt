package qbt.vcs;

import java.util.Collection;
import misc1.commons.Either;
import qbt.VcsTreeDigest;

public interface TreeAccessor {
    public default TreeAccessor replace(String path, byte[] contents) {
        return replace(path, Either.right(contents));
    }

    public default TreeAccessor replace(String path, TreeAccessor contents) {
        return replace(path, Either.left(contents));
    }

    public TreeAccessor replace(String path, Either<TreeAccessor, byte[]> contents);
    public Either<TreeAccessor, byte[]> get(String path);
    public TreeAccessor remove(String path);
    public VcsTreeDigest getDigest();
    public boolean isEmpty();
    public Collection<String> getEntryNames();
}
