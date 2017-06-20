package qbt.manifest.current;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Map;
import misc1.commons.Maybe;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructBuilder;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import misc1.commons.ds.StructTypeBuilder;
import misc1.commons.json.JsonSerializer;
import misc1.commons.json.JsonSerializers;
import qbt.manifest.PackageBuildType;
import qbt.manifest.QbtJsonSerializers;

public final class PackageMetadata extends Struct<PackageMetadata, PackageMetadata.Builder> {
    private PackageMetadata(ImmutableMap<StructKey<PackageMetadata, ?, ?>, Object> map) {
        super(TYPE, map);
    }

    public static class Builder extends StructBuilder<PackageMetadata, Builder> {
        public Builder(ImmutableSalvagingMap<StructKey<PackageMetadata, ?, ?>, Object> map) {
            super(TYPE, map);
        }
    }

    public static final StructKey<PackageMetadata, Boolean, Boolean> ARCH_INDEPENDENT;
    public static final StructKey<PackageMetadata, PackageBuildType, PackageBuildType> BUILD_TYPE;
    public static final StructKey<PackageMetadata, String, String> PREFIX;
    public static final StructKey<PackageMetadata, ImmutableMap<String, Maybe<String>>, ImmutableMap<String, Maybe<String>>> QBT_ENV;
    public static final StructType<PackageMetadata, Builder> TYPE;
    static {
        StructTypeBuilder<PackageMetadata, Builder> b = new StructTypeBuilder<>(PackageMetadata::new, Builder::new);

        ARCH_INDEPENDENT = b.<Boolean>key("archIndependent").def(false).serializer(JsonSerializers.BOOLEAN).add();
        BUILD_TYPE = b.<PackageBuildType>key("buildType").def(PackageBuildType.NORMAL).serializer(JsonSerializers.forEnum(PackageBuildType.class)).add();
        PREFIX = b.<String>key("prefix").def("").serializer(JsonSerializers.STRING).add();
        QBT_ENV = b.<ImmutableMap<String, Maybe<String>>>key("qbtEnv").def(ImmutableMap.<String, Maybe<String>>of()).serializer(QbtJsonSerializers.QBT_ENV).add();

        TYPE = b.build();
    }

    public static final JsonSerializer<Builder> SERIALIZER = new JsonSerializer<Builder>() {
        @Override
        public JsonElement toJson(Builder b) {
            JsonObject r = new JsonObject();
            Boolean archIndependent = b.get(ARCH_INDEPENDENT);
            if(!archIndependent.equals(false)) {
                r.add("archIndependent", (JsonSerializers.BOOLEAN).toJson(archIndependent));
            }
            PackageBuildType buildType = b.get(BUILD_TYPE);
            if(!buildType.equals(PackageBuildType.NORMAL)) {
                r.add("buildType", (JsonSerializers.forEnum(PackageBuildType.class)).toJson(buildType));
            }
            String prefix = b.get(PREFIX);
            if(!prefix.equals("")) {
                r.add("prefix", new JsonPrimitive(prefix));
            }
            ImmutableMap<String, Maybe<String>> qbtEnv = b.get(QBT_ENV);
            if(!qbtEnv.equals(ImmutableMap.<String, Maybe<String>>of())) {
                r.add("qbtEnv", (QbtJsonSerializers.QBT_ENV).toJson(qbtEnv));
            }
            return r;
        }

        @Override
        public Builder fromJson(JsonElement e) {
            Builder b = TYPE.builder();
            for(Map.Entry<String, JsonElement> e2 : e.getAsJsonObject().entrySet()) {
                switch(e2.getKey()) {
                    case "archIndependent":
                        b = b.set(ARCH_INDEPENDENT, (JsonSerializers.BOOLEAN).fromJson(e2.getValue()));
                        break;

                    case "buildType":
                        b = b.set(BUILD_TYPE, (JsonSerializers.forEnum(PackageBuildType.class)).fromJson(e2.getValue()));
                        break;

                    case "prefix":
                        b = b.set(PREFIX, e2.getValue().getAsString());
                        break;

                    case "qbtEnv":
                        b = b.set(QBT_ENV, (QbtJsonSerializers.QBT_ENV).fromJson(e2.getValue()));
                        break;

                    default:
                        throw new IllegalArgumentException(e2.getKey());
                }
            }
            return b;
        }
    };
}
