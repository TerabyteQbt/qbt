package qbt.config;

import qbt.remote.QbtRemote;

public interface QbtRemoteFinder {
    QbtRemote findQbtRemote(String remote);
    default QbtRemote requireQbtRemote(String remote) {
        QbtRemote r = findQbtRemote(remote);
        if(r == null) {
            throw new IllegalArgumentException("Could not qbt remote for " + remote);
        }
        return r;
    }
}
