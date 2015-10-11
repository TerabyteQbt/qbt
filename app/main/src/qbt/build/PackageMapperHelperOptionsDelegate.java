package qbt.build;

import com.google.common.collect.ImmutableList;
import misc1.commons.Maybe;
import misc1.commons.options.NamedBooleanFlagOptionsFragment;
import misc1.commons.options.NamedStringSingletonArgumentOptionsFragment;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsFragment;
import qbt.options.ParallelismOptionsDelegate;

public class PackageMapperHelperOptionsDelegate<O> implements OptionsDelegate<O> {
    public final ParallelismOptionsDelegate<O> parallelism = new ParallelismOptionsDelegate<O>();
    public final OptionsFragment<O, ?, String> arch = new NamedStringSingletonArgumentOptionsFragment<O>(ImmutableList.of("--cache-architecture"), Maybe.<String>of(null), "Architecture for cache get/put");
    public final OptionsFragment<O, ?, Boolean> noBuilds = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--no-builds"), "Refuse actual builds (only allow cache hits)");
}
