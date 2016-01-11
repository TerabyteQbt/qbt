package qbt;

import com.google.common.collect.ImmutableList;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;

public interface QbtCommandOptions {
    public static final OptionsLibrary<QbtCommandOptions> o = OptionsLibrary.of();
    public static final OptionsFragment<QbtCommandOptions, ?> help = o.zeroArg("h", "help").transform(o.help()).helpDesc("Show help");
    public static final OptionsFragment<QbtCommandOptions, String> logProperties = o.oneArg("logProperties").transform(o.singleton(null)).helpDesc("Configuring logging from properties");
    public static final OptionsFragment<QbtCommandOptions, ImmutableList<String>> logLevels = o.oneArg("logLevel").helpDesc("Set log level, e.g.  DEBUG (sets root), my.Class=INFO (sets specific category)");
    public static final OptionsFragment<QbtCommandOptions, String> logFormat = o.oneArg("logFormat").transform(o.singleton("%m%n")).helpDesc("Set log format");
}
