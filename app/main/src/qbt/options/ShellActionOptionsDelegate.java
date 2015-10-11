package qbt.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;
import misc1.commons.Maybe;
import misc1.commons.options.NamedBooleanFlagOptionsFragment;
import misc1.commons.options.NamedStringSingletonArgumentOptionsFragment;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import misc1.commons.options.UnparsedOptionsFragment;

public class ShellActionOptionsDelegate<O> implements OptionsDelegate<O> {
    public final OptionsFragment<O, ?, Boolean> isInteractive = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--interactive"), "Treat the command as interactive");
    public final OptionsFragment<O, ?, Boolean> interactiveShell = new NamedBooleanFlagOptionsFragment<O>(ImmutableList.of("--interactive-shell"), "Run an interactive $SHELL");
    public final OptionsFragment<O, ?, String> shell = new NamedStringSingletonArgumentOptionsFragment<O>(ImmutableList.of("--shell"), Maybe.<String>of(null), "Command to be run with $SHELL -c");
    public final OptionsFragment<O, ?, ImmutableList<String>> command = new UnparsedOptionsFragment<O>("Command to run", true, null, null);

    private List<ShellActionOptionsResult> choices(OptionsResults<? extends O> options) {
        boolean optionsIsInteractive = options.get(isInteractive);
        Boolean optionsInteractiveShell = options.get(interactiveShell);
        String optionsShell = options.get(shell);
        ImmutableList<String> optionsCommand = options.get(command);

        ImmutableList.Builder<ShellActionOptionsResult> b = ImmutableList.builder();

        if(optionsCommand.size() > 0) {
            b.add(new ShellActionOptionsResult(optionsCommand, optionsIsInteractive));
        }

        if(optionsShell != null) {
            b.add(new ShellActionOptionsResult(ImmutableList.of(System.getenv("SHELL"), "-c", optionsShell), optionsIsInteractive));
        }

        if(optionsInteractiveShell) {
            b.add(new ShellActionOptionsResult(ImmutableList.of(System.getenv("SHELL"), "-i"), true));
        }

        return b.build();
    }

    public ShellActionOptionsResult getResultsOptional(OptionsResults<? extends O> options) {
        List<ShellActionOptionsResult> choices = choices(options);
        if(choices.size() == 0) {
            return null;
        }
        if(choices.size() > 1) {
            throw new IllegalArgumentException("Must specify only one command!");
        }
        return Iterables.getOnlyElement(choices);
    }

    public ShellActionOptionsResult getResults(OptionsResults<? extends O> options) {
        ShellActionOptionsResult ret = getResultsOptional(options);
        if(ret == null) {
            throw new IllegalArgumentException("Must specify a command!");
        }
        return ret;
    }
}
