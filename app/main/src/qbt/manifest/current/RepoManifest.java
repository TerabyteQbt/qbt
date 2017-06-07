package qbt.manifest.current;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructBuilder;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import misc1.commons.ds.StructTypeBuilder;
import misc1.commons.json.JsonSerializer;
import qbt.VcsVersionDigest;
import qbt.manifest.QbtJsonSerializers;

public final class RepoManifest extends Struct<RepoManifest, RepoManifest.Builder> {
    public final Optional<VcsVersionDigest> version;
    public final ImmutableMap<String, PackageManifest> packages;

    private RepoManifest(ImmutableMap<StructKey<RepoManifest, ?, ?>, Object> map) {
        super(TYPE, map);

        this.version = get(VERSION);
        this.packages = get(PACKAGES).map;
    }

    public static class Builder extends StructBuilder<RepoManifest, Builder> {
        public Builder(ImmutableSalvagingMap<StructKey<RepoManifest, ?, ?>, Object> map) {
            super(TYPE, map);
        }
    }

    public static final StructKey<RepoManifest, RepoManifestPackages, RepoManifestPackages.Builder> PACKAGES;
    public static final StructKey<RepoManifest, Optional<VcsVersionDigest>, Optional<VcsVersionDigest>> VERSION;
    public static final StructType<RepoManifest, Builder> TYPE;
    static {
        StructTypeBuilder<RepoManifest, Builder> b = new StructTypeBuilder<>(RepoManifest::new, Builder::new);

        PACKAGES = b.key("packages", RepoManifestPackages.TYPE).add();
        VERSION = b.<Optional<VcsVersionDigest>>key("version").add();

        TYPE = b.build();
    }

    public static final JsonSerializer<Builder> SERIALIZER = new JsonSerializer<Builder>() {
        @Override
        public JsonElement toJson(Builder b) {
            JsonObject r = new JsonObject();
            RepoManifestPackages.Builder packages = b.get(PACKAGES);
            if(!packages.equals(RepoManifestPackages.TYPE.builder())) {
                r.add("packages", (RepoManifestPackages.SERIALIZER).toJson(packages));
            }
            r.add("version", (QbtJsonSerializers.OPTIONAL_VCS_VERSION_DIGEST).toJson(b.get(VERSION)));
            return r;
        }

        @Override
        public Builder fromJson(JsonElement e) {
            Builder b = TYPE.builder();
            for(Map.Entry<String, JsonElement> e2 : e.getAsJsonObject().entrySet()) {
                switch(e2.getKey()) {
                    case "packages":
                        b = b.set(PACKAGES, (RepoManifestPackages.SERIALIZER).fromJson(e2.getValue()));
                        break;

                    case "version":
                        b = b.set(VERSION, (QbtJsonSerializers.OPTIONAL_VCS_VERSION_DIGEST).fromJson(e2.getValue()));
                        break;

                    default:
                        throw new IllegalArgumentException(e2.getKey());
                }
            }
            return b;
        }
    };
}
