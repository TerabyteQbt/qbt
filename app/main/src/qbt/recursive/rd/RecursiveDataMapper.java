package qbt.recursive.rd;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import misc1.commons.Pointer;

public abstract class RecursiveDataMapper<EDGE_KEY, EDGE_VALUE, NODE_VALUE, R extends RecursiveData<NODE_VALUE, EDGE_KEY, EDGE_VALUE, R>, OUTPUT> {
    private final LoadingCache<Pointer<R>, OUTPUT> cache = CacheBuilder.newBuilder().build(new CacheLoader<Pointer<R>, OUTPUT>() {
        @Override
        public OUTPUT load(Pointer<R> pr) throws Exception {
            return map(pr.value);
        }
    });

    public OUTPUT transform(R r) {
        return cache.getUnchecked(Pointer.of(r));
    }

    protected abstract OUTPUT map(R r);
}
