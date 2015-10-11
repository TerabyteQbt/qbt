package qbt;

import com.google.common.hash.HashCode;

public abstract class TypedDigest {
    HashCode delegate;

    protected TypedDigest(HashCode delegate) {
        this.delegate = delegate;
    }

    @Override
    public final boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(!getClass().equals(obj.getClass())) {
            return false;
        }
        TypedDigest other = (TypedDigest) obj;
        return delegate.equals(other.delegate);
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode() ^ delegate.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + delegate;
    }

    public HashCode getRawDigest() {
        return delegate;
    }
}
