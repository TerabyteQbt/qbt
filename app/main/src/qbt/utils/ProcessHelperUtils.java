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
package qbt.utils;

import misc1.commons.ph.ProcessHelper;

public final class ProcessHelperUtils {
    private ProcessHelperUtils() {
        // nope
    }

    public static ProcessHelper stripGitEnv(ProcessHelper p) {
        // Fuckers, these break all sorts of things and are also insane.  `git`
        // is so bad at handling environment variables if `qbt` is run
        // underneath git at any point (e.g.  mergeDriver) all hell breaks
        // loose.
        p = p.removeEnv("GIT_DIR");
        p = p.removeEnv("GIT_WORK_TREE");
        return p;
    }

    public static ProcessHelper.Callback<?> simplePrefixCallback(final String prefix) {
        return new ProcessHelper.Callback<Void>() {
            @Override
            public void line(boolean isError, String line) {
                (isError ? System.err : System.out).println("[" + prefix + "] " + line);
            }

            @Override
            public Void complete(int exitCode) {
                if(exitCode != 0) {
                    throw new RuntimeException("[" + prefix + "] Non-zero exit: " + exitCode);
                }
                return null;
            }
        };
    }
}
