package qbt.config;

import qbt.remote.QbtRemote;

public class EmptyQbtRemoteFinder implements QbtRemoteFinder {
    @Override
    public QbtRemote findQbtRemote(String remote) {
        return null;
    }
}
