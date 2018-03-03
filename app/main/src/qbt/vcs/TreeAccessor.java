package qbt.vcs;

import java.util.Collection;
import misc1.commons.Either;
import qbt.QbtUtils;
import qbt.VcsTreeDigest;

public interface TreeAccessor {
    public default Iterable<String> requireFileLines(String path) {
        Either<TreeAccessor, byte[]> e = get(path);
        if(e == null || e.isLeft()) {
            throw new IllegalArgumentException("No " + path);
        }
        return QbtUtils.bytesToLines(e.rightOrNull());
    }

    public default TreeAccessor replace(String path, Iterable<String> lines) {
        return replace(path, QbtUtils.linesToBytes(lines));
    }

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
