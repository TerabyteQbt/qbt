package qbt.diffmanifests;

import java.util.Comparator;
import java.util.Map;

public abstract class NoEditMapDiffer<K, V> extends MapDiffer<K, V> {
    public NoEditMapDiffer(Map<K, V> lhs, Map<K, V> rhs, Comparator<K> cmp) {
        super(lhs, rhs, cmp);
    }

    @Override
    protected void edit(K key, V lhs, V rhs) {
        del(key, lhs);
        add(key, rhs);
    }
}
