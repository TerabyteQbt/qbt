package qbt.config;

import qbt.remote.QbtRemote;

public class EmptyQbtRemoteFinder extends AbstractQbtRemoteFinder {
    @Override
    public QbtRemote findQbtRemote(String remote) {
        return null;
    }
}
