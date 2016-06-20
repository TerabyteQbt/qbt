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
package qbt.recursive.cvrpd;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.cv.CumulativeVersionNodeData;
import qbt.recursive.rpd.RecursivePackageData;
import qbt.recursive.rpd.RecursivePackageDataTransformer;

public abstract class CvRecursivePackageDataVersionAdder<V, R extends RecursivePackageData<V, R>> extends RecursivePackageDataTransformer<V, R, Pair<CumulativeVersion, V>, CvRecursivePackageData<V>> {
    private final Function<Pair<NormalDependencyType, CvRecursivePackageData<V>>, Pair<NormalDependencyType, CumulativeVersion>> simplify = (input) -> Pair.of(input.getLeft(), input.getRight().v);

    @Override
    protected CvRecursivePackageData<V> transformResult(R r, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V>>> dependencyResults) {
        CumulativeVersion v = CumulativeVersion.of(nodeData(r.result), Maps.transformValues(dependencyResults, simplify));
        return new CvRecursivePackageData<V>(v, r.result, dependencyResults);
    }

    protected abstract CumulativeVersionNodeData nodeData(V result);
}
