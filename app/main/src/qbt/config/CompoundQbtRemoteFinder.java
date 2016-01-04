package qbt.config;

import com.google.common.collect.ImmutableList;
import qbt.remote.QbtRemote;

public final class CompoundQbtRemoteFinder implements QbtRemoteFinder {
    private final ImmutableList<QbtRemoteFinder> children;

    public CompoundQbtRemoteFinder(Iterable<QbtRemoteFinder> children) {
        this.children = ImmutableList.copyOf(children);
    }

    @Override
    public QbtRemote findQbtRemote(String remote) {
        for(QbtRemoteFinder child : children) {
            QbtRemote r = child.findQbtRemote(remote);
            if(r != null) {
                return r;
            }
        }
        return null;
    }
}
