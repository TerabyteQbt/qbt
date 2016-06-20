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
