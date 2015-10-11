package qbt.diffmanifests;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;

public abstract class MapDiffer<K, V> {
    private final Map<K, V> lhs;
    private final Map<K, V> rhs;
    private final Comparator<K> cmp;

    public MapDiffer(Map<K, V> lhs, Map<K, V> rhs, Comparator<K> cmp) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.cmp = cmp;
    }

    public void diff() {
        TreeSet<K> keys = Sets.newTreeSet(cmp);
        keys.addAll(lhs.keySet());
        keys.addAll(rhs.keySet());

        for(K key : keys) {
            diffKey(key, lhs.get(key), rhs.get(key));
        }
    }

    private void diffKey(K key, V lhs, V rhs) {
        if(lhs == null) {
            if(rhs == null) {
                // ?
            }
            else {
                add(key, rhs);
            }
        }
        else {
            if(rhs == null) {
                del(key, lhs);
            }
            else {
                if(!Objects.equal(lhs, rhs)) {
                    edit(key, lhs, rhs);
                }
            }
        }
    }

    protected abstract void edit(K key, V lhs, V rhs);
    protected abstract void add(K key, V value);
    protected abstract void del(K key, V value);
}
