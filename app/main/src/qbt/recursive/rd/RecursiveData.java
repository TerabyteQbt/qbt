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
import org.apache.commons.lang3.tuple.Pair;

public class RecursiveData<NODE_VALUE, EDGE_KEY, EDGE_VALUE, R extends RecursiveData<NODE_VALUE, EDGE_KEY, EDGE_VALUE, R>> {
    public final NODE_VALUE result;
    public final Map<EDGE_KEY, Pair<EDGE_VALUE, R>> children;

    public RecursiveData(NODE_VALUE result, Map<EDGE_KEY, Pair<EDGE_VALUE, R>> children) {
        this.result = result;
        this.children = children;
    }
}
