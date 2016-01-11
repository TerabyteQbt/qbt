package qbt.options;

import misc1.commons.concurrent.WorkPool;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;

public class ParallelismOptionsDelegate<O> implements OptionsDelegate<O> {
    private final OptionsLibrary<O> o = OptionsLibrary.of();
    public final OptionsFragment<O, Integer> parallelism = o.oneArg("parallelism", "j").transform(o.singleton(null)).transform(o.parseInt()).helpDesc("Parallelize up to this width");
    public final OptionsFragment<O, Boolean> infiniteParallelism = o.zeroArg("infinite-parallelism", "J").transform(o.flag()).helpDesc("Parallelize as widely as possible");

    public ParallelismOptionsResult getResult(OptionsResults<? extends O> options, boolean isInteractive) {
        if(isInteractive) {
            return () -> WorkPool.explicitParallelism(1);
        }
        if(options.get(infiniteParallelism)) {
            return WorkPool::infiniteParallelism;
        }
        Integer explicitParallelism = options.get(parallelism);
        if(explicitParallelism != null) {
            return () -> WorkPool.explicitParallelism(explicitParallelism);
        }
        return WorkPool::defaultParallelism;
    }
}
