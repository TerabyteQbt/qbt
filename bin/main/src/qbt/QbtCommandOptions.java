package qbt;

import com.google.common.collect.ImmutableList;
import misc1.commons.Maybe;
import misc1.commons.options.HelpOptionsFragment;
import misc1.commons.options.NamedStringListArgumentOptionsFragment;
import misc1.commons.options.NamedStringSingletonArgumentOptionsFragment;
import misc1.commons.options.OptionsFragment;

public interface QbtCommandOptions {
    public static final OptionsFragment<QbtCommandOptions, ?, ?> help = new HelpOptionsFragment<QbtCommandOptions>(ImmutableList.of("-h", "--help"), "Show help");
    public static final OptionsFragment<QbtCommandOptions, ?, String> logProperties = new NamedStringSingletonArgumentOptionsFragment<QbtCommandOptions>(ImmutableList.of("--logProperties"), Maybe.<String>of(null), "Configuring logging from properties");
    public static final OptionsFragment<QbtCommandOptions, ?, ImmutableList<String>> logLevels = new NamedStringListArgumentOptionsFragment<QbtCommandOptions>(ImmutableList.of("--logLevel"), "Set log level, e.g.  DEBUG (sets root), my.Class=INFO (sets specific category)");
    public static final OptionsFragment<QbtCommandOptions, ?, String> logFormat = new NamedStringSingletonArgumentOptionsFragment<QbtCommandOptions>(ImmutableList.of("--logFormat"), Maybe.of("%m%n"), "Set log format");
}
