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
import misc1.commons.json.JsonSerializers;
import misc1.commons.json.StringSerializer;
import misc1.commons.merge.Merge;
import misc1.commons.merge.Merges;
import qbt.tip.PackageTip;

public final class PackageReplaceDeps extends MapStruct<PackageReplaceDeps, PackageReplaceDeps.Builder, PackageTip, String, String> {
    private PackageReplaceDeps(ImmutableMap<PackageTip, String> map) {
        super(TYPE, map);
    }

    public static class Builder extends MapStructBuilder<PackageReplaceDeps, Builder, PackageTip, String, String> {
        public Builder(ImmutableSalvagingMap<PackageTip, String> map) {
            super(TYPE, map);
        }
    }

    public static final MapStructType<PackageReplaceDeps, Builder, PackageTip, String, String> TYPE = new MapStructType<PackageReplaceDeps, Builder, PackageTip, String, String>() {
        @Override
        protected PackageReplaceDeps create(ImmutableMap<PackageTip, String> map) {
            return new PackageReplaceDeps(map);
        }

        @Override
        protected Builder createBuilder(ImmutableSalvagingMap<PackageTip, String> map) {
            return new Builder(map);
        }

        @Override
        protected String toStruct(String vb) {
            return vb;
        }

        @Override
        protected String toBuilder(String vs) {
            return vs;
        }

        @Override
        protected Merge<String> mergeValue() {
            return Merges.<String>trivial();
        }

        @Override
        protected Optional<StringSerializer<PackageTip>> keySerializer() {
            return Optional.of(PackageTip.TYPE.STRING_SERIALIZER);
        }

        @Override
        protected Optional<JsonSerializer<String>> valueSerializer() {
            return Optional.of(JsonSerializers.STRING);
        }
    };

    public static final JsonSerializer<Builder> SERIALIZER = new JsonSerializer<Builder>() {
        @Override
        public JsonElement toJson(Builder b) {
            JsonObject r = new JsonObject();
            for(Map.Entry<PackageTip, String> e : b.map.entries()) {
                r.add(PackageTip.TYPE.STRING_SERIALIZER.toString(e.getKey()), JsonSerializers.STRING.toJson(e.getValue()));
            }
            return r;
        }

        @Override
        public Builder fromJson(JsonElement e) {
            Builder b = TYPE.builder();
            for(Map.Entry<String, JsonElement> e2 : e.getAsJsonObject().entrySet()) {
                b = b.with(PackageTip.TYPE.STRING_SERIALIZER.fromString(e2.getKey()), JsonSerializers.STRING.fromJson(e2.getValue()));
            }
            return b;
        }
    };
}
