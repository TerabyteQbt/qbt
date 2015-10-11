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
