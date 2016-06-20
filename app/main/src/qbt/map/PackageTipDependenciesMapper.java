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
package qbt.map;

import com.google.common.collect.ImmutableList;
import misc1.commons.ds.LazyCollector;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.map.DependencyComputer;
import qbt.recursive.srpd.SimpleRecursivePackageData;
import qbt.recursive.srpd.SimpleRecursivePackageDataMapper;
import qbt.tip.PackageTip;

public class PackageTipDependenciesMapper extends SimpleRecursivePackageDataMapper<DependencyComputer.Result, LazyCollector<PackageTip>> {
    @Override
    protected LazyCollector<PackageTip> map(SimpleRecursivePackageData<DependencyComputer.Result> r) {
        ImmutableList.Builder<LazyCollector<PackageTip>> b = ImmutableList.builder();
        for(Pair<NormalDependencyType, SimpleRecursivePackageData<DependencyComputer.Result>> e : r.children.values()) {
            b.add(transform(e.getRight()));
        }
        b.add(LazyCollector.of(r.result.packageTip));
        return LazyCollector.unionIterable(b.build());
    }
}
