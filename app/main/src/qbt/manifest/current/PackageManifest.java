package qbt.manifest.current;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructBuilder;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import misc1.commons.ds.StructTypeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
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
        StructTypeBuilder<PackageManifest, Builder> b = new StructTypeBuilder<>(PackageManifest::new, Builder::new);

        METADATA = b.key("metadata", PackageMetadata.TYPE).add();
        NORMAL_DEPS = b.key("normalDeps", PackageNormalDeps.TYPE).add();
        REPLACE_DEPS = b.key("replaceDeps", PackageReplaceDeps.TYPE).add();
        VERIFY_DEPS = b.key("verifyDeps", PackageVerifyDeps.TYPE).add();

        TYPE = b.build();
    }
}
