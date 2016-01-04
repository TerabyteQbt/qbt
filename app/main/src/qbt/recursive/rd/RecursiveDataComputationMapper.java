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
