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
package qbt.map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsException;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;

public class CumulativeVersionComputerOptionsDelegate<O> implements OptionsDelegate<O> {
    private final OptionsLibrary<O> o = OptionsLibrary.of();
    public final OptionsFragment<O, ImmutableList<String>> qbtEnv = o.oneArg("qbtEnv").helpDesc("Qbt environment variables");

    public CumulativeVersionComputerOptionsResult getResults(OptionsResults<? extends O> options) {
        ImmutableMap.Builder<String, String> qbtEnvBuilder = ImmutableMap.builder();
        for(String s : options.get(qbtEnv)) {
            int i = s.indexOf('=');
            if(i == -1) {
                throw new OptionsException("Qbt environment argument did not have name and value: " + s);
            }
            qbtEnvBuilder.put(s.substring(0, i), s.substring(i + 1));
        }
        return new CumulativeVersionComputerOptionsResult(qbtEnvBuilder.build());
    }
}
