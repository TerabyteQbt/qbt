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
        return LazyCollector.unionIterable(b.build());
    }
}
