package qbt.manifest;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import java.util.Optional;
import misc1.commons.Maybe;
import misc1.commons.json.JsonSerializer;
import misc1.commons.json.JsonSerializers;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.VcsVersionDigest;

public final class QbtJsonSerializers {
    private QbtJsonSerializers() {
        // nope
    }

    public static final JsonSerializer<Optional<VcsVersionDigest>> OPTIONAL_VCS_VERSION_DIGEST = new JsonSerializer<Optional<VcsVersionDigest>>() {
        @Override
        public JsonElement toJson(Optional<VcsVersionDigest> commit) {
            if(commit.isPresent()) {
                return new JsonPrimitive(VcsVersionDigest.DEPARSE_FUNCTION.apply(commit.get()));
            }
            return JsonNull.INSTANCE;
        }

        @Override
        public Optional<VcsVersionDigest> fromJson(JsonElement e) {
            if(e.isJsonNull()) {
                return Optional.empty();
            }
            return Optional.of(VcsVersionDigest.PARSE_FUNCTION.apply(e.getAsString()));
        }
    };

    public static final JsonSerializer<Pair<NormalDependencyType, String>> NORMAL_DEP_VALUE = new JsonSerializer<Pair<NormalDependencyType, String>>() {
        @Override
        public JsonElement toJson(Pair<NormalDependencyType, String> pair) {
            return new JsonPrimitive(pair.getLeft().getTag() + "," + pair.getRight());
        }

        @Override
        public Pair<NormalDependencyType, String> fromJson(JsonElement e) {
            String[] a = e.getAsString().split(",");
            if(a.length != 2) {
                throw new IllegalArgumentException();
            }
            return Pair.of(NormalDependencyType.fromTag(a[0]), a[1]);
        }
    };

    public static final JsonSerializer<ImmutableMap<String, Maybe<String>>> QBT_ENV = JsonSerializers.map(JsonSerializers.MAYBE_STRING);
}
