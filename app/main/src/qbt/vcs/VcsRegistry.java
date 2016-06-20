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
package qbt.vcs;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import qbt.vcs.git.GitLocalVcs;
import qbt.vcs.git.GitRawRemoteVcs;

public final class VcsRegistry {
    private VcsRegistry() {
        // nope
    }

    private static final Map<String, LocalVcs> LOCAL_VCS;
    static {
        ImmutableMap.Builder<String, LocalVcs> b = ImmutableMap.builder();

        b.put("git", new GitLocalVcs());

        LOCAL_VCS = b.build();
    }

    private static final Map<String, RawRemoteVcs> RAW_REMOTE_VCS;
    static {
        ImmutableMap.Builder<String, RawRemoteVcs> b = ImmutableMap.builder();

        b.put("git", new GitRawRemoteVcs());

        RAW_REMOTE_VCS = b.build();
    }

    private static <T> T get(Class<T> clazz, Map<String, T> registry, String name) {
        T ret = registry.get(name);
        if(ret == null) {
            throw new IllegalArgumentException("No such " + clazz.getSimpleName() + ": " + name);
        }
        return ret;
    }

    public static LocalVcs getLocalVcs(String vcs) {
        return get(LocalVcs.class, LOCAL_VCS, vcs);
    }

    public static RawRemoteVcs getRawRemoteVcs(String vcs) {
        return get(RawRemoteVcs.class, RAW_REMOTE_VCS, vcs);
    }
}
