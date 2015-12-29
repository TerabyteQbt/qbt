package qbt.options;

import misc1.commons.concurrent.WorkPool;
import misc1.commons.concurrent.ctree.ComputationTree;

public interface ParallelismOptionsResult {
    WorkPool createWorkPool();
    <T> T runComputationTree(ComputationTree<T> computationTree);
}
