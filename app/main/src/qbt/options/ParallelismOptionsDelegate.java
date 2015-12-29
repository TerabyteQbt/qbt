package qbt.options;

import com.google.common.collect.ImmutableList;
import misc1.commons.Maybe;
import misc1.commons.concurrent.WorkPool;
import misc1.commons.concurrent.ctree.ComputationTree;
import misc1.commons.concurrent.ctree.ComputationTreeComputer;
import misc1.commons.options.NamedBooleanFlagOptionsFragment;
import misc1.commons.options.NamedIntegerSingletonArgumentOptionsFragment;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;

public class ParallelismOptionsDelegate<O> implements OptionsDelegate<O> {
    public final OptionsFragment<O, ?, Integer> parallelism = new NamedIntegerSingletonArgumentOptionsFragment<O>(ImmutableList.of("--parallelism", "-j"), Maybe.<Integer>of(null), "Parallelize up to this width");
    public final OptionsFragment<O, ?, Boolean> infiniteParallelism = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--infinite-parallelism", "-J"), "Parallelize as widely as possible");

    private static abstract class BaseParallelismOptionsResult implements ParallelismOptionsResult {
        @Override
        public <T> T runComputationTree(ComputationTree<T> computationTree) {
            try(WorkPool workPool = createWorkPool()) {
                return new ComputationTreeComputer(workPool).await(computationTree).getCommute();
            }
        }
    }

    private static class ExplicitParallelismOptionsResult extends BaseParallelismOptionsResult {
        private final int parallelism;

        public ExplicitParallelismOptionsResult(int parallelism) {
            this.parallelism = parallelism;
        }

        @Override
        public WorkPool createWorkPool() {
            return WorkPool.explicitParallelism(parallelism);
        }
    }

    private static class InfiniteParallelismOptionsResult extends BaseParallelismOptionsResult {
        @Override
        public WorkPool createWorkPool() {
            return WorkPool.infiniteParallelism();
        }
    }

    private static class DefaultParallelismOptionsResult extends BaseParallelismOptionsResult {
        @Override
        public WorkPool createWorkPool() {
            return WorkPool.defaultParallelism();
        }
    }

    public ParallelismOptionsResult getResult(OptionsResults<? extends O> options, boolean isInteractive) {
        if(isInteractive) {
            return new ExplicitParallelismOptionsResult(1);
        }
        if(options.get(infiniteParallelism)) {
            return new InfiniteParallelismOptionsResult();
        }
        Integer explicitParallelism = options.get(parallelism);
        if(explicitParallelism != null) {
            return new ExplicitParallelismOptionsResult(explicitParallelism);
        }
        return new DefaultParallelismOptionsResult();
    }
}
