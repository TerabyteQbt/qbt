//   Copyright 2016 Keith Amling
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
package qbt.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;

public class ShellActionOptionsDelegate<O> implements OptionsDelegate<O> {
    private final OptionsLibrary<O> o = OptionsLibrary.of();
    public final OptionsFragment<O, Boolean> isInteractive = o.zeroArg("interactive").transform(o.flag()).helpDesc("Treat the command as interactive");
    public final OptionsFragment<O, Boolean> interactiveShell = o.zeroArg("interactive-shell").transform(o.flag()).helpDesc("Run an interactive $SHELL");
    public final OptionsFragment<O, String> shell = o.oneArg("shell").transform(o.singleton(null)).helpDesc("Command to be run with $SHELL -c");
    public final OptionsFragment<O, ImmutableList<String>> command = o.unparsed(true).helpDesc("Command to run");

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
