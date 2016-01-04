package qbt.recursive.utils;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import misc1.commons.concurrent.ctree.ComputationTree;
import org.apache.commons.lang3.tuple.Pair;

public final class RecursiveDataUtils {
    private RecursiveDataUtils() {
        // hidden
    }

    public static <EDGE_KEY, EDGE_VALUE, V, W> Map<EDGE_KEY, Pair<EDGE_VALUE, W>> transformMap(Map<EDGE_KEY, Pair<EDGE_VALUE, V>> map, Function<V, W> fn) {
        ImmutableMap.Builder<EDGE_KEY, Pair<EDGE_VALUE, W>> b = ImmutableMap.builder();
        for(Map.Entry<EDGE_KEY, Pair<EDGE_VALUE, V>> e : map.entrySet()) {
            b.put(e.getKey(), Pair.of(e.getValue().getLeft(), fn.apply(e.getValue().getRight())));
        }
        return b.build();
    }

    public static <EDGE_KEY, EDGE_VALUE, V> ComputationTree<Map<EDGE_KEY, Pair<EDGE_VALUE, V>>> computationTreeMap(Map<EDGE_KEY, Pair<EDGE_VALUE, ComputationTree<V>>> map) {
        return computationTreeMap(map, Functions.<Map<EDGE_KEY, Pair<EDGE_VALUE, V>>>identity());
    }

    public static <EDGE_KEY, EDGE_VALUE, V, W> ComputationTree<W> computationTreeMap(Map<EDGE_KEY, Pair<EDGE_VALUE, ComputationTree<V>>> map, final Function<Map<EDGE_KEY, Pair<EDGE_VALUE, V>>, W> fn) {
        ImmutableList.Builder<EDGE_KEY> edgeKeysBuilder = ImmutableList.builder();
        ImmutableList.Builder<EDGE_VALUE> edgeValuesBuilder = ImmutableList.builder();
        ImmutableList.Builder<ComputationTree<V>> childComputationTreesBuilder = ImmutableList.builder();
        for(Map.Entry<EDGE_KEY, Pair<EDGE_VALUE, ComputationTree<V>>> e : map.entrySet()) {
            edgeKeysBuilder.add(e.getKey());
            edgeValuesBuilder.add(e.getValue().getLeft());
            childComputationTreesBuilder.add(e.getValue().getRight());
        }
        final ImmutableList<EDGE_KEY> edgeKeys = edgeKeysBuilder.build();
        final ImmutableList<EDGE_VALUE> edgeValues = edgeValuesBuilder.build();
        return ComputationTree.list(childComputationTreesBuilder.build()).transform((childResultsList) -> {
            ImmutableMap.Builder<EDGE_KEY, Pair<EDGE_VALUE, V>> childResultsBuilder = ImmutableMap.builder();
            for(int i = 0; i < childResultsList.size(); ++i) {
                childResultsBuilder.put(edgeKeys.get(i), Pair.of(edgeValues.get(i), childResultsList.get(i)));
            }
            return fn.apply(childResultsBuilder.build());
        });
    }
}
