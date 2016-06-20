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
