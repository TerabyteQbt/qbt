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

import java.util.Map;
import misc1.commons.concurrent.ctree.ComputationTree;
import org.apache.commons.lang3.tuple.Pair;
import qbt.recursive.utils.RecursiveDataUtils;

public abstract class RecursiveDataComputationMapper<EDGE_KEY, EDGE_VALUE, NODE_VALUE, R extends RecursiveData<NODE_VALUE, EDGE_KEY, EDGE_VALUE, R>, OUTPUT> extends RecursiveDataMapper<EDGE_KEY, EDGE_VALUE, NODE_VALUE, R, ComputationTree<OUTPUT>> {
    @Override
    protected ComputationTree<OUTPUT> map(final R r) {
        Map<EDGE_KEY, Pair<EDGE_VALUE, ComputationTree<OUTPUT>>> childComputatonTrees = RecursiveDataUtils.transformMap(r.children, this::transform);
        return RecursiveDataUtils.computationTreeMap(childComputatonTrees, (input) -> map(r, input));
    }

    protected abstract OUTPUT map(R r, Map<EDGE_KEY, Pair<EDGE_VALUE, OUTPUT>> childResults);
}
