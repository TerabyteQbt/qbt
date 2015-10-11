package qbt.options;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class ShellActionOptionsResult {
    public final List<String> command;
    public final String[] commandArray;
    public final boolean isInteractive;

    public ShellActionOptionsResult(List<String> command, boolean isInteractive) {
        this.command = ImmutableList.copyOf(command);
        this.commandArray = command.toArray(new String[0]);
        this.isInteractive = isInteractive;
    }
}
