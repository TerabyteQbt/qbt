package qbt.diffmanifests;

import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public abstract class SetDiffer<K> {
    private final Set<K> lhs;
    private final Set<K> rhs;
    private final Comparator<K> cmp;

    public SetDiffer(Set<K> lhs, Set<K> rhs, Comparator<K> cmp) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.cmp = cmp;
    }

    public void diff() {
        TreeSet<K> keys = Sets.newTreeSet(cmp);
        keys.addAll(lhs);
        keys.addAll(rhs);

        for(K key : keys) {
            boolean l = lhs.contains(key);
            boolean r = rhs.contains(key);
            if(l && !r) {
                del(key);
            }
            if(r && !l) {
                add(key);
            }
        }
    }

    protected abstract void add(K key);
    protected abstract void del(K key);
}
