//   Copyright 2016 Keith Amling
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
package qbt.recursive.rd;

import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import misc1.commons.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;

public abstract class RecursiveDataCanonicalizer<NODE_VALUE, EDGE_KEY, EDGE_VALUE, R extends RecursiveData<NODE_VALUE, EDGE_KEY, EDGE_VALUE, R>, K> extends RecursiveDataTransformer<EDGE_KEY, EDGE_VALUE, NODE_VALUE, R, NODE_VALUE, R> {
    private static final class CacheKey<EDGE_KEY, EDGE_VALUE, R, K> {
        private final K k;
        private final Map<EDGE_KEY, Pair<EDGE_VALUE, R>> children;

        public CacheKey(K k, Map<EDGE_KEY, Pair<EDGE_VALUE, R>> children) {
            this.k = k;
            this.children = children;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(k, children);
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof CacheKey)) {
                return false;
            }
            CacheKey<?, ?, ?, ?> other = (CacheKey<?, ?, ?, ?>)obj;
            if(!Objects.equal(k, other.k)) {
                return false;
            }
            if(!children.equals(other.children)) {
                return false;
            }
            return true;
        }
    }

    private final Cache<CacheKey<EDGE_KEY, EDGE_VALUE, R, K>, R> cache = CacheBuilder.newBuilder().build();

    @Override
    protected R transformResult(final R r, final Map<EDGE_KEY, Pair<EDGE_VALUE, R>> children) {
        CacheKey<EDGE_KEY, EDGE_VALUE, R, K> key = new CacheKey<EDGE_KEY, EDGE_VALUE, R, K>(key(r), children);
        try {
            return cache.get(key, () -> newR(r.result, children));
        }
        catch(ExecutionException e) {
            throw ExceptionUtils.commute(e.getCause());
        }
    }

    protected abstract K key(R r);
    protected abstract R newR(NODE_VALUE result, Map<EDGE_KEY, Pair<EDGE_VALUE, R>> children);
}
