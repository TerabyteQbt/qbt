package qbt.manifest.current;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.MapStruct;
import misc1.commons.ds.MapStructBuilder;
import misc1.commons.ds.MapStructType;
import misc1.commons.json.JsonSerializer;
import misc1.commons.json.StringSerializers;
import misc1.commons.merge.Merge;

public final class RepoManifestPackages extends MapStruct<RepoManifestPackages, RepoManifestPackages.Builder, String, PackageManifest, PackageManifest.Builder> {
    private RepoManifestPackages(ImmutableMap<String, PackageManifest> map) {
        super(TYPE, map);
    }

    public static class Builder extends MapStructBuilder<RepoManifestPackages, Builder, String, PackageManifest, PackageManifest.Builder> {
        public Builder(ImmutableSalvagingMap<String, PackageManifest.Builder> map) {
            super(TYPE, map);
        }
    }

    public static final MapStructType<RepoManifestPackages, Builder, String, PackageManifest, PackageManifest.Builder> TYPE = new MapStructType<RepoManifestPackages, Builder, String, PackageManifest, PackageManifest.Builder>() {
        @Override
        protected RepoManifestPackages create(ImmutableMap<String, PackageManifest> map) {
            return new RepoManifestPackages(map);
        }

        @Override
        protected Builder createBuilder(ImmutableSalvagingMap<String, PackageManifest.Builder> map) {
            return new Builder(map);
        }

        @Override
        protected PackageManifest toStruct(PackageManifest.Builder vb) {
            return (vb).build();
        }

        @Override
        protected PackageManifest.Builder toBuilder(PackageManifest vs) {
            return (vs).builder();
        }

        @Override
        protected Merge<PackageManifest> mergeValue() {
            return PackageManifest.TYPE.merge();
        }
    };

    public static final JsonSerializer<Builder> SERIALIZER = new JsonSerializer<Builder>() {
        @Override
        public JsonElement toJson(Builder b) {
            JsonObject r = new JsonObject();
            for(Map.Entry<String, PackageManifest.Builder> e : b.map.entries()) {
                r.add(StringSerializers.STRING.toString(e.getKey()), PackageManifest.SERIALIZER.toJson(e.getValue()));
            }
            return r;
        }

        @Override
        public Builder fromJson(JsonElement e) {
            Builder b = TYPE.builder();
            for(Map.Entry<String, JsonElement> e2 : e.getAsJsonObject().entrySet()) {
                b = b.with(StringSerializers.STRING.fromString(e2.getKey()), PackageManifest.SERIALIZER.fromJson(e2.getValue()));
            }
            return b;
        }
    };
}
