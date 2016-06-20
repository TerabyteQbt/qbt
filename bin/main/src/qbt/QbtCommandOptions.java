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
