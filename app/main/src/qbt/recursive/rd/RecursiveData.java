package qbt.recursive.rd;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public class RecursiveData<NODE_VALUE, EDGE_KEY, EDGE_VALUE, R extends RecursiveData<NODE_VALUE, EDGE_KEY, EDGE_VALUE, R>> {
    public final NODE_VALUE result;
    public final Map<EDGE_KEY, Pair<EDGE_VALUE, R>> children;

    public RecursiveData(NODE_VALUE result, Map<EDGE_KEY, Pair<EDGE_VALUE, R>> children) {
        this.result = result;
        this.children = children;
    }
}
