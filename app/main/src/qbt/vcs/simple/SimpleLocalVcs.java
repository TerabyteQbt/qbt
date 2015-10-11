package qbt.vcs.simple;

import qbt.vcs.LocalVcs;

public abstract class SimpleLocalVcs implements LocalVcs {
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && getClass().equals(obj.getClass());
    }
}
