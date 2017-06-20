package qbt.manifest.current;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.MapStruct;
import misc1.commons.ds.MapStructBuilder;
import misc1.commons.ds.MapStructType;
import misc1.commons.json.JsonSerializer;
import misc1.commons.json.StringSerializer;
import misc1.commons.json.StringSerializers;
import misc1.commons.merge.Merge;
import misc1.commons.merge.Merges;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.manifest.QbtJsonSerializers;

public final class PackageNormalDeps extends MapStruct<PackageNormalDeps, PackageNormalDeps.Builder, String, Pair<NormalDependencyType, String>, Pair<NormalDependencyType, String>> {
    private PackageNormalDeps(ImmutableMap<String, Pair<NormalDependencyType, String>> map) {
        super(TYPE, map);
    }

    public static class Builder extends MapStructBuilder<PackageNormalDeps, Builder, String, Pair<NormalDependencyType, String>, Pair<NormalDependencyType, String>> {
        public Builder(ImmutableSalvagingMap<String, Pair<NormalDependencyType, String>> map) {
            super(TYPE, map);
        }
    }

    public static final MapStructType<PackageNormalDeps, Builder, String, Pair<NormalDependencyType, String>, Pair<NormalDependencyType, String>> TYPE = new MapStructType<PackageNormalDeps, Builder, String, Pair<NormalDependencyType, String>, Pair<NormalDependencyType, String>>() {
        @Override
        protected PackageNormalDeps create(ImmutableMap<String, Pair<NormalDependencyType, String>> map) {
            return new PackageNormalDeps(map);
        }

        @Override
        protected Builder createBuilder(ImmutableSalvagingMap<String, Pair<NormalDependencyType, String>> map) {
            return new Builder(map);
        }

        @Override
        protected Pair<NormalDependencyType, String> toStruct(Pair<NormalDependencyType, String> vb) {
            return vb;
        }

        @Override
        protected Pair<NormalDependencyType, String> toBuilder(Pair<NormalDependencyType, String> vs) {
            return vs;
        }

        @Override
        protected Merge<Pair<NormalDependencyType, String>> mergeValue() {
            return Merges.<Pair<NormalDependencyType, String>>trivial();
        }

        @Override
        protected Optional<StringSerializer<String>> keySerializer() {
            return Optional.of(StringSerializers.STRING);
        }

        @Override
        protected Optional<JsonSerializer<Pair<NormalDependencyType, String>>> valueSerializer() {
            return Optional.of(QbtJsonSerializers.NORMAL_DEP_VALUE);
        }
    };

    public static final JsonSerializer<Builder> SERIALIZER = new JsonSerializer<Builder>() {
        @Override
        public JsonElement toJson(Builder b) {
            JsonObject r = new JsonObject();
            for(Map.Entry<String, Pair<NormalDependencyType, String>> e : b.map.entries()) {
                r.add(StringSerializers.STRING.toString(e.getKey()), QbtJsonSerializers.NORMAL_DEP_VALUE.toJson(e.getValue()));
            }
            return r;
        }

        @Override
        public Builder fromJson(JsonElement e) {
            Builder b = TYPE.builder();
            for(Map.Entry<String, JsonElement> e2 : e.getAsJsonObject().entrySet()) {
                b = b.with(StringSerializers.STRING.fromString(e2.getKey()), QbtJsonSerializers.NORMAL_DEP_VALUE.fromJson(e2.getValue()));
            }
            return b;
        }
    };
}
