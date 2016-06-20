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
package qbt.mains;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.Locale;
import java.util.Map;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.QbtCommands;
import qbt.QbtMain;

public class Help extends QbtCommand<Help.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Help.class);

    @QbtCommandName("help")
    public static interface Options extends QbtCommandOptions {
        public static final OptionsLibrary<Options> o = OptionsLibrary.of();
        public static final OptionsFragment<Options, ImmutableList<String>> command = o.unparsed(false).transform(o.minMax(0, 1)).helpDesc("Command to show help for");
        public static final OptionsFragment<Options, Boolean> allTiers = o.zeroArg("all", "a").transform(o.flag()).helpDesc("Show all command tiers, not just the more common ones");
    }

    @Override
    public Class<Options> getOptionsClass() {
        return Options.class;
    }

    @Override
    public HelpTier getHelpTier() {
        return HelpTier.COMMON;
    }

    @Override
    public String getDescription() {
        return "print this message";
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws Exception {
        String command = Iterables.getOnlyElement(options.get(Options.command), null);
        if(command != null) {
            QbtCommand<?> instance = QbtCommands.getCommands().get(command);
            if(instance != null) {
                return QbtMain.runInstanceHelp(instance);
            }
            LOGGER.error("Invalid command: " + command);
            return 1;
        }

        Map<HelpTier, Map<String, Optional<String>>> sortedCommands = Maps.newTreeMap();
        for(Map.Entry<String, QbtCommand<?>> e : QbtCommands.getCommands().entrySet()) {
            QbtCommand<?> instance = e.getValue();
            HelpTier helpTier = instance.getHelpTier();
            Map<String, Optional<String>> tierSortedCommands = sortedCommands.get(helpTier);
            if(tierSortedCommands == null) {
                sortedCommands.put(helpTier, tierSortedCommands = Maps.newTreeMap());
            }
            tierSortedCommands.put(e.getKey(), Optional.fromNullable(instance.getDescription()));
        }

        boolean verbose = options.get(Options.allTiers);

        for(Map.Entry<HelpTier, Map<String, Optional<String>>> e : sortedCommands.entrySet()) {
            HelpTier helpTier = e.getKey();
            if(!helpTier.showByDefault && !verbose) {
                continue;
            }
            LOGGER.info("Available " + helpTier.name().toLowerCase(Locale.US) + " commands:");
            for(Map.Entry<String, Optional<String>> e2 : e.getValue().entrySet()) {
                Optional<String> description = e2.getValue();
                LOGGER.info("   " + e2.getKey() + (description.isPresent() ? (" - " + description.get()) : ""));
            }
        }

        return 0;
    }
}
