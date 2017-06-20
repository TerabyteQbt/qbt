package qbt.manifest.current;

import com.google.common.collect.ImmutableMap;
import misc1.commons.Maybe;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructBuilder;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import misc1.commons.ds.StructTypeBuilder;
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
}
