package qbt.manifest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import misc1.commons.Maybe;

public final class ExternalUpgrades {
    private ExternalUpgrades() {
        // no
    }

    public static ImmutableMap<String, Maybe<String>> qbtEnv(ImmutableSet<String> old) {
        ImmutableMap.Builder<String, Maybe<String>> b = ImmutableMap.builder();
        for(String key : old) {
            b.put(key, Maybe.not());
        }
        return b.build();
    }
}
