package qbt.config;

import qbt.remote.QbtRemote;

public abstract class AbstractQbtRemoteFinder implements QbtRemoteFinder {
    @Override
    public QbtRemote requireQbtRemote(String remote) {
        QbtRemote r = findQbtRemote(remote);
        if(r == null) {
            throw new IllegalArgumentException("Could not qbt remote for " + remote);
        }
        return r;
    }
}
