package qbt;

import com.google.common.hash.HashCode;

public final class VcsTreeDigest extends TypedDigest {
    public VcsTreeDigest(HashCode delegate) {
        super(delegate);
    }
}
