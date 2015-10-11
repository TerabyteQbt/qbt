package qbt;

import com.google.common.collect.ImmutableList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import misc1.commons.options.HelpRequestedException;
import misc1.commons.options.OptionsException;
import misc1.commons.options.OptionsResults;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import qbt.mains.Help;

public final class QbtMain {
    private QbtMain() {
        // no
    }

    public static void main(String[] args) throws Exception {
        if(args.length < 1) {
            args = new String[] {"help"};
        }
        String command = args[0];
        QbtCommand<?> instance = QbtCommands.getCommands().get(command);
        List<String> instanceArgs;
        if(instance == null) {
            instanceArgs = ImmutableList.of("--", command);
            command = "help";
            instance = new Help();
        }
        else {
            instanceArgs = Arrays.asList(args).subList(1, args.length);
        }

        String qbtRcFile = System.getenv("QBTRC");
        Path qbtRc = qbtRcFile != null ? Paths.get(qbtRcFile) : (Paths.get(System.getenv("HOME")).resolve(".qbtrc"));
        if(Files.isRegularFile(qbtRc)) {
            ImmutableList.Builder<String> b = ImmutableList.builder();
            for(String line : QbtUtils.readLines(qbtRc)) {
                // not strictly necessary due to weak check below, but clearer
                if(line.startsWith("#")) {
                    continue;
                }
                if(line.equals("")) {
                    continue;
                }
                for(String prefix : ImmutableList.of("ALL", command)) {
                    if(line.startsWith(prefix + ":")) {
                        b.add(line.substring(prefix.length() + 1));
                    }
                }
            }
            b.addAll(instanceArgs);
            instanceArgs = b.build();
        }

        int exitCode = runInstance(instance, instanceArgs, true);

        if(exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static <O extends QbtCommandOptions> int runInstance(QbtCommand<O> r, List<String> args, boolean configureLogging) throws Exception {
        try {
            OptionsResults<O> options = OptionsResults.parse(r.getOptionsClass(), args);
            if(configureLogging) {
                String logProperties = options.get(QbtCommandOptions.logProperties);
                if(logProperties != null) {
                    PropertyConfigurator.configure(logProperties);
                }
                else {
                    EnhancedPatternLayout layout = new EnhancedPatternLayout(options.get(QbtCommandOptions.logFormat));
                    ConsoleAppender console = new ConsoleAppender(layout, r.isProgrammaticOutput() ? ConsoleAppender.SYSTEM_ERR : ConsoleAppender.SYSTEM_OUT);
                    console.activateOptions();
                    Logger rootLogger = Logger.getRootLogger();
                    rootLogger.addAppender(console);
                    rootLogger.setLevel(r.isProgrammaticOutput() ? Level.ERROR : Level.INFO);
                }
                for(String logLevel : options.get(QbtCommandOptions.logLevels)) {
                    int i = logLevel.lastIndexOf('=');
                    Logger logger;
                    String levelString;
                    if(i == -1) {
                        logger = Logger.getRootLogger();
                        levelString = logLevel;
                    }
                    else {
                        logger = Logger.getLogger(logLevel.substring(0, i));
                        levelString = logLevel.substring(i + 1);
                    }
                    logger.setLevel(Level.toLevel(levelString));
                }
            }

            return r.run(options);
        }
        catch(HelpRequestedException e) {
            return runInstanceHelp(r);
        }
        catch(OptionsException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }

    public static <O extends QbtCommandOptions> int runInstanceHelp(QbtCommand<O> r) {
        System.err.println("Usage: " + QbtCommands.getName(r) + " ARGS");
        for(String line : OptionsResults.help(r.getOptionsClass())) {
            System.err.println("   " + line);
        }
        return 1;
    }
}
