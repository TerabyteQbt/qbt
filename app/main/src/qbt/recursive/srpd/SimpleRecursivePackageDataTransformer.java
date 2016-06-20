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
package qbt.recursive.srpd;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.rpd.RecursivePackageDataTransformer;

public abstract class SimpleRecursivePackageDataTransformer<V1, V2> extends RecursivePackageDataTransformer<V1, SimpleRecursivePackageData<V1>, V2, SimpleRecursivePackageData<V2>> {
    @Override
    protected SimpleRecursivePackageData<V2> transformResult(SimpleRecursivePackageData<V1> r, Map<String, Pair<NormalDependencyType, SimpleRecursivePackageData<V2>>> dependencyResults) {
        return new SimpleRecursivePackageData<V2>(transformResult(r.result, dependencyResults), dependencyResults);
    }

    protected abstract V2 transformResult(V1 result, Map<String, Pair<NormalDependencyType, SimpleRecursivePackageData<V2>>> dependencyResults);
}
