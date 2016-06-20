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
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.rpd.RecursivePackageData;

public class CvRecursivePackageData<V> extends RecursivePackageData<Pair<CumulativeVersion, V>, CvRecursivePackageData<V>> {
    public final CumulativeVersion v;

    public CvRecursivePackageData(CumulativeVersion v, V result, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V>>> children) {
        this(Pair.of(v, result), children);
    }

    public CvRecursivePackageData(Pair<CumulativeVersion, V> result, Map<String, Pair<NormalDependencyType, CvRecursivePackageData<V>>> children) {
        super(result, children);
        this.v = result.getLeft();
    }

    public static <V> Function<CvRecursivePackageData<V>, V> innerValueFunction() {
        return (input) -> input.result.getRight();
    }
}
