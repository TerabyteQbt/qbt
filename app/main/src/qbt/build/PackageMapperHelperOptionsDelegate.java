package qbt.build;

import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import qbt.options.ParallelismOptionsDelegate;

public class PackageMapperHelperOptionsDelegate<O> implements OptionsDelegate<O> {
    private final OptionsLibrary<O> o = OptionsLibrary.of();
    public final ParallelismOptionsDelegate<O> parallelism = new ParallelismOptionsDelegate<O>();
    public final OptionsFragment<O, String> arch = o.oneArg("cache-architecture").transform(o.singleton(null)).helpDesc("Architecture for cache get/put");
    public final OptionsFragment<O, Boolean> noBuilds = o.zeroArg("no-builds").transform(o.flag()).helpDesc("Refuse actual builds (only allow cache hits)");
}
