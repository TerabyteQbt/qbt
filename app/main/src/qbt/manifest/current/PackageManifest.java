package qbt.manifest.current;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructBuilder;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import misc1.commons.merge.Merge;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.manifest.JsonSerializer;
import qbt.tip.PackageTip;

public final class PackageManifest extends Struct<PackageManifest, PackageManifest.Builder> {
    public final PackageMetadata metadata;
    public final ImmutableMap<String, Pair<NormalDependencyType, String>> normalDeps;
    public final ImmutableMap<PackageTip, String> replaceDeps;
    public final ImmutableSet<Pair<PackageTip, String>> verifyDeps;

    private PackageManifest(ImmutableMap<StructKey<PackageManifest, ?, ?>, Object> map) {
        super(TYPE, map);

        this.metadata = get(METADATA);
        this.normalDeps = get(NORMAL_DEPS).map;
        this.replaceDeps = get(REPLACE_DEPS).map;
        this.verifyDeps = get(VERIFY_DEPS).map.keySet();
    }

    public static class Builder extends StructBuilder<PackageManifest, Builder> {
        public Builder(ImmutableSalvagingMap<StructKey<PackageManifest, ?, ?>, Object> map) {
            super(TYPE, map);
        }
    }

    public static final StructKey<PackageManifest, PackageMetadata, PackageMetadata.Builder> METADATA;
    public static final StructKey<PackageManifest, PackageNormalDeps, PackageNormalDeps.Builder> NORMAL_DEPS;
    public static final StructKey<PackageManifest, PackageReplaceDeps, PackageReplaceDeps.Builder> REPLACE_DEPS;
    public static final StructKey<PackageManifest, PackageVerifyDeps, PackageVerifyDeps.Builder> VERIFY_DEPS;
    public static final StructType<PackageManifest, Builder> TYPE;
    static {
        ImmutableList.Builder<StructKey<PackageManifest, ?, ?>> b = ImmutableList.builder();

        b.add(METADATA = new StructKey<PackageManifest, PackageMetadata, PackageMetadata.Builder>("metadata", PackageMetadata.TYPE.builder()) {
            @Override
            public PackageMetadata toStruct(PackageMetadata.Builder vb) {
                return (vb).build();
            }

            @Override
            public PackageMetadata.Builder toBuilder(PackageMetadata vs) {
                return (vs).builder();
            }

            @Override
            public Merge<PackageMetadata> merge() {
                return PackageMetadata.TYPE.merge();
            }
        });
        b.add(NORMAL_DEPS = new StructKey<PackageManifest, PackageNormalDeps, PackageNormalDeps.Builder>("normalDeps", PackageNormalDeps.TYPE.builder()) {
            @Override
            public PackageNormalDeps toStruct(PackageNormalDeps.Builder vb) {
                return (vb).build();
            }

            @Override
            public PackageNormalDeps.Builder toBuilder(PackageNormalDeps vs) {
                return (vs).builder();
            }

            @Override
            public Merge<PackageNormalDeps> merge() {
                return PackageNormalDeps.TYPE.merge();
            }
        });
        b.add(REPLACE_DEPS = new StructKey<PackageManifest, PackageReplaceDeps, PackageReplaceDeps.Builder>("replaceDeps", PackageReplaceDeps.TYPE.builder()) {
            @Override
            public PackageReplaceDeps toStruct(PackageReplaceDeps.Builder vb) {
                return (vb).build();
            }

            @Override
            public PackageReplaceDeps.Builder toBuilder(PackageReplaceDeps vs) {
                return (vs).builder();
            }

            @Override
            public Merge<PackageReplaceDeps> merge() {
                return PackageReplaceDeps.TYPE.merge();
            }
        });
        b.add(VERIFY_DEPS = new StructKey<PackageManifest, PackageVerifyDeps, PackageVerifyDeps.Builder>("verifyDeps", PackageVerifyDeps.TYPE.builder()) {
            @Override
            public PackageVerifyDeps toStruct(PackageVerifyDeps.Builder vb) {
                return (vb).build();
            }

            @Override
            public PackageVerifyDeps.Builder toBuilder(PackageVerifyDeps vs) {
                return (vs).builder();
            }

            @Override
            public Merge<PackageVerifyDeps> merge() {
                return PackageVerifyDeps.TYPE.merge();
            }
        });

        TYPE = new StructType<PackageManifest, Builder>(b.build(), PackageManifest::new, Builder::new);
    }

    public static final JsonSerializer<Builder> SERIALIZER = new JsonSerializer<Builder>() {
        @Override
        public JsonElement toJson(Builder b) {
            JsonObject r = new JsonObject();
            PackageMetadata.Builder metadata = b.get(METADATA);
            if(!metadata.equals(PackageMetadata.TYPE.builder())) {
                r.add("metadata", (PackageMetadata.SERIALIZER).toJson(metadata));
            }
            PackageNormalDeps.Builder normalDeps = b.get(NORMAL_DEPS);
            if(!normalDeps.equals(PackageNormalDeps.TYPE.builder())) {
                r.add("normalDeps", (PackageNormalDeps.SERIALIZER).toJson(normalDeps));
            }
            PackageReplaceDeps.Builder replaceDeps = b.get(REPLACE_DEPS);
            if(!replaceDeps.equals(PackageReplaceDeps.TYPE.builder())) {
                r.add("replaceDeps", (PackageReplaceDeps.SERIALIZER).toJson(replaceDeps));
            }
            PackageVerifyDeps.Builder verifyDeps = b.get(VERIFY_DEPS);
            if(!verifyDeps.equals(PackageVerifyDeps.TYPE.builder())) {
                r.add("verifyDeps", (PackageVerifyDeps.SERIALIZER).toJson(verifyDeps));
            }
            return r;
        }

        @Override
        public Builder fromJson(JsonElement e) {
            Builder b = TYPE.builder();
            for(Map.Entry<String, JsonElement> e2 : e.getAsJsonObject().entrySet()) {
                switch(e2.getKey()) {
                    case "metadata":
                        b = b.set(METADATA, (PackageMetadata.SERIALIZER).fromJson(e2.getValue()));
                        break;

                    case "normalDeps":
                        b = b.set(NORMAL_DEPS, (PackageNormalDeps.SERIALIZER).fromJson(e2.getValue()));
                        break;

                    case "replaceDeps":
                        b = b.set(REPLACE_DEPS, (PackageReplaceDeps.SERIALIZER).fromJson(e2.getValue()));
                        break;

                    case "verifyDeps":
                        b = b.set(VERIFY_DEPS, (PackageVerifyDeps.SERIALIZER).fromJson(e2.getValue()));
                        break;

                    default:
                        throw new IllegalArgumentException(e2.getKey());
                }
            }
            return b;
        }
    };
}
