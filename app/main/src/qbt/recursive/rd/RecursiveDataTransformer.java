package qbt.recursive.rd;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public abstract class RecursiveDataTransformer<EDGE_KEY, EDGE_VALUE, NODE_VALUE1, R1 extends RecursiveData<NODE_VALUE1, EDGE_KEY, EDGE_VALUE, R1>, NODE_VALUE2, R2 extends RecursiveData<NODE_VALUE2, EDGE_KEY, EDGE_VALUE, R2>> extends RecursiveDataMapper<EDGE_KEY, EDGE_VALUE, NODE_VALUE1, R1, R2> {
    @Override
    protected R2 map(R1 r) {
        ImmutableMap.Builder<EDGE_KEY, Pair<EDGE_VALUE, R2>> dependencyResultsBuilder = ImmutableMap.builder();
        for(Map.Entry<EDGE_KEY, Pair<EDGE_VALUE, R1>> e : r.children.entrySet()) {
            EDGE_KEY edgeKey = e.getKey();
            Pair<EDGE_VALUE, R1> edgePair = e.getValue();
            EDGE_VALUE edgeValue = edgePair.getLeft();
            R1 child = edgePair.getRight();
            if(keepLink(r.result, edgeKey, edgeValue, child)) {
                dependencyResultsBuilder.put(edgeKey, Pair.of(edgeValue, transform(child)));
            }
        }
        return transformResult(r, dependencyResultsBuilder.build());
    }

    protected boolean keepLink(NODE_VALUE1 result, EDGE_KEY edgeKey, EDGE_VALUE edgeValue, R1 child) {
        return true;
    }

    protected abstract R2 transformResult(R1 r, Map<EDGE_KEY, Pair<EDGE_VALUE, R2>> children);
}
