package qbt.vcs.simple;

import qbt.vcs.RawRemoteVcs;

public abstract class SimpleRawRemoteVcs implements RawRemoteVcs {
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && getClass().equals(obj.getClass());
    }
}
