package qbt.options;

import misc1.commons.concurrent.WorkPool;
import misc1.commons.concurrent.ctree.ComputationTree;
import misc1.commons.concurrent.ctree.ComputationTreeComputer;

public interface ParallelismOptionsResult {
    WorkPool createWorkPool();
    default <T> T runComputationTree(ComputationTree<T> computationTree) {
        try(WorkPool workPool = createWorkPool()) {
            return new ComputationTreeComputer(workPool).await(computationTree).getCommute();
        }
    }
}
