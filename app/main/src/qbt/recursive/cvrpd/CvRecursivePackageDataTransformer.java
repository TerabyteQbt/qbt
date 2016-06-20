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
import qbt.recursive.rpd.RecursivePackageDataTransformer;

public abstract class CvRecursivePackageDataTransformer<V1, V2> extends RecursivePackageDataTransformer<Pair<CumulativeVersion, V1>, CvRecursivePackageData<V1>, Pair<CumulativeVersion, V2>, CvRecursivePackageData<V2>> {
    @Override
    protected boolean keepLink(Pair<CumulativeVersion, V1> result, String dependencyName, NormalDependencyType dependencyType, CvRecursivePackageData<V1> dependencyResult) {
        return keepLink(result.getLeft(), result.getRight(), dependencyName, dependencyType, dependencyResult);
    }

    private final Function<Pair<NormalDependencyType, CvRecursivePackageData<V2>>, Pair<NormalDependencyType, CumulativeVersion>> getDependencyVersion = (input) -> Pair.of(input.getLeft(), input.getRight().v);
    @Override
    protected CvRecursivePackageData<V2> transformResult(CvRecursivePackageData<V1> r, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V2>>> dependencyResults) {
        CumulativeVersion v = CumulativeVersion.of(transformNodeData(r.v.result), Maps.transformValues(dependencyResults, getDependencyVersion));
        return new CvRecursivePackageData<V2>(v, transformResult(r.v, v, r.result.getRight(), dependencyResults), dependencyResults);
    }

    protected CumulativeVersionNodeData transformNodeData(CumulativeVersionNodeData nodeData) {
        return nodeData;
    }

    protected boolean keepLink(CumulativeVersion v, V1 result, String dependencyName, NormalDependencyType dependencyType, CvRecursivePackageData<V1> dependencyResult) {
        return true;
    }

    protected abstract V2 transformResult(CumulativeVersion vOld, CumulativeVersion vNew, V1 result, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V2>>> dependencyResults);
}
