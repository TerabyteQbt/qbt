package qbt.config;

import qbt.remote.QbtRemote;

public interface QbtRemoteFinder {
    public QbtRemote findQbtRemote(String remote);
    public QbtRemote requireQbtRemote(String remote);
}
