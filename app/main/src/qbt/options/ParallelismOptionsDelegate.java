package qbt.options;

import com.google.common.collect.ImmutableList;
import misc1.commons.Maybe;
import misc1.commons.concurrent.WorkPool;
import misc1.commons.options.NamedBooleanFlagOptionsFragment;
import misc1.commons.options.NamedIntegerSingletonArgumentOptionsFragment;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;

public class ParallelismOptionsDelegate<O> implements OptionsDelegate<O> {
    public final OptionsFragment<O, ?, Integer> parallelism = new NamedIntegerSingletonArgumentOptionsFragment<O>(ImmutableList.of("--parallelism", "-j"), Maybe.<Integer>of(null), "Parallelize up to this width");
    public final OptionsFragment<O, ?, Boolean> infiniteParallelism = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--infinite-parallelism", "-J"), "Parallelize as widely as possible");

    public ParallelismOptionsResult getResult(OptionsResults<? extends O> options, boolean isInteractive) {
        if(isInteractive) {
            return () -> WorkPool.explicitParallelism(1);
        }
        if(options.get(infiniteParallelism)) {
            return () -> WorkPool.infiniteParallelism();
        }
        Integer explicitParallelism = options.get(parallelism);
        if(explicitParallelism != null) {
            return () -> WorkPool.explicitParallelism(explicitParallelism);
        }
        return () -> WorkPool.defaultParallelism();
    }
}
