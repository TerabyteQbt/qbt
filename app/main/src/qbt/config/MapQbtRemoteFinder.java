package qbt.config;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import qbt.remote.QbtRemote;

public final class MapQbtRemoteFinder extends AbstractQbtRemoteFinder {
    private final ImmutableMap<String, QbtRemote> map;

    public MapQbtRemoteFinder(Map<String, QbtRemote> map) {
        this.map = ImmutableMap.copyOf(map);
    }

    @Override
    public QbtRemote findQbtRemote(String remote) {
        return map.get(remote);
    }
}
