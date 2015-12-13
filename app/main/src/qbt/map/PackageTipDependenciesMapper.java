package qbt.map;

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
        LazyCollector<PackageTip> ret = LazyCollector.of();
        for(Pair<NormalDependencyType, SimpleRecursivePackageData<DependencyComputer.Result>> e : r.children.values()) {
            ret = ret.union(transform(e.getRight()));
        }
        ret = ret.union(LazyCollector.of(r.result.packageTip));
        return ret;
    }
}
