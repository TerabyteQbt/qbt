package qbt.manifest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructBuilder;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import misc1.commons.merge.Merge;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.metadata.Metadata;
import qbt.metadata.MetadataItem;
import qbt.metadata.PackageMetadataType;
import qbt.tip.PackageTip;

public final class PackageManifest extends Struct<PackageManifest, PackageManifest.Builder> {
    public final Metadata<PackageMetadataType> metadata;
    public final ImmutableMap<String, Pair<NormalDependencyType, String>> normalDeps;
    public final ImmutableMap<PackageTip, String> replaceDeps;
    public final ImmutableMap<Pair<PackageTip, String>, ObjectUtils.Null> verifyDeps;

    private PackageManifest(ImmutableMap<StructKey<PackageManifest, ?, ?>, Object> map) {
        super(TYPE, map);
        this.metadata = get(METADATA);
        this.normalDeps = get(NORMAL_DEPS).map;
        this.replaceDeps = get(REPLACE_DEPS).map;
        this.verifyDeps = get(VERIFY_DEPS).map;
    }

    public static class Builder extends StructBuilder<PackageManifest, Builder> {
        public Builder(ImmutableSalvagingMap<StructKey<PackageManifest, ?, ?>, Object> map) {
            super(TYPE, map);
        }

        public <T> Builder withMetadata(MetadataItem<PackageMetadataType, T> item, T value) {
            Metadata.Builder<PackageMetadataType> b = get(METADATA);
            b = b.put(item, value);
            return set(METADATA, b);
        }

        public Builder withStringMetadata(String item, String value) {
            Metadata.Builder<PackageMetadataType> b = get(METADATA);
            b = b.putString(item, value);
            return set(METADATA, b);
        }

        public Builder withNormalDep(PackageTip p, NormalDependencyType type) {
            PackageManifestNormalDeps.Builder b = get(NORMAL_DEPS);
            b = b.with(p.name, Pair.of(type, p.tip));
            return set(NORMAL_DEPS, b);
        }

        public Builder withReplaceDep(PackageTip p, String s) {
            PackageManifestReplaceDeps.Builder b = get(REPLACE_DEPS);
            b = b.with(p, s);
            return set(REPLACE_DEPS, b);
        }

        public Builder withVerifyDep(PackageTip p, String s) {
            PackageManifestVerifyDeps.Builder b = get(VERIFY_DEPS);
            b = b.with(Pair.of(p, s), ObjectUtils.NULL);
            return set(VERIFY_DEPS, b);
        }
    }

    public static final StructKey<PackageManifest, Metadata<PackageMetadataType>, Metadata.Builder<PackageMetadataType>> METADATA;
    public static final StructKey<PackageManifest, PackageManifestNormalDeps, PackageManifestNormalDeps.Builder> NORMAL_DEPS;
    public static final StructKey<PackageManifest, PackageManifestReplaceDeps, PackageManifestReplaceDeps.Builder> REPLACE_DEPS;
    public static final StructKey<PackageManifest, PackageManifestVerifyDeps, PackageManifestVerifyDeps.Builder> VERIFY_DEPS;
    public static final StructType<PackageManifest, Builder> TYPE;
    static {
        ImmutableList.Builder<StructKey<PackageManifest, ?, ?>> b = ImmutableList.builder();

        b.add(METADATA = new StructKey<PackageManifest, Metadata<PackageMetadataType>, Metadata.Builder<PackageMetadataType>>("metadata", PackageMetadataType.of().builder()) {
            @Override
            public Metadata<PackageMetadataType> toStruct(Metadata.Builder<PackageMetadataType> vb) {
                return vb.build();
            }

            @Override
            public Metadata.Builder<PackageMetadataType> toBuilder(Metadata<PackageMetadataType> vs) {
                return vs.builder();
            }

            @Override
            public Merge<Metadata<PackageMetadataType>> merge() {
                return Metadata.merge(PackageMetadataType.INSTANCE);
            }
        });
        b.add(NORMAL_DEPS = new StructKey<PackageManifest, PackageManifestNormalDeps, PackageManifestNormalDeps.Builder>("normalDeps", PackageManifestNormalDeps.TYPE.builder()) {
            @Override
            public PackageManifestNormalDeps toStruct(PackageManifestNormalDeps.Builder vb) {
                return vb.build();
            }

            @Override
            public PackageManifestNormalDeps.Builder toBuilder(PackageManifestNormalDeps  vs) {
                return vs.builder();
            }

            @Override
            public Merge<PackageManifestNormalDeps> merge() {
                return PackageManifestNormalDeps.TYPE.merge();
            }
        });
        b.add(REPLACE_DEPS = new StructKey<PackageManifest, PackageManifestReplaceDeps, PackageManifestReplaceDeps.Builder>("replaceDeps", PackageManifestReplaceDeps.TYPE.builder()) {
            @Override
            public PackageManifestReplaceDeps toStruct(PackageManifestReplaceDeps.Builder vb) {
                return vb.build();
            }

            @Override
            public PackageManifestReplaceDeps.Builder toBuilder(PackageManifestReplaceDeps  vs) {
                return vs.builder();
            }

            @Override
            public Merge<PackageManifestReplaceDeps> merge() {
                return PackageManifestReplaceDeps.TYPE.merge();
            }
        });
        b.add(VERIFY_DEPS = new StructKey<PackageManifest, PackageManifestVerifyDeps, PackageManifestVerifyDeps.Builder>("verifyDeps", PackageManifestVerifyDeps.TYPE.builder()) {
            @Override
            public PackageManifestVerifyDeps toStruct(PackageManifestVerifyDeps.Builder vb) {
                return vb.build();
            }

            @Override
            public PackageManifestVerifyDeps.Builder toBuilder(PackageManifestVerifyDeps  vs) {
                return vs.builder();
            }

            @Override
            public Merge<PackageManifestVerifyDeps> merge() {
                return PackageManifestVerifyDeps.TYPE.merge();
            }
        });

        TYPE = new StructType<PackageManifest, Builder>(b.build()) {
            @Override
            protected PackageManifest createUnchecked(ImmutableMap<StructKey<PackageManifest, ?, ?>, Object> map) {
                return new PackageManifest(map);
            }

            @Override
            protected PackageManifest.Builder createBuilder(ImmutableSalvagingMap<StructKey<PackageManifest, ?, ?>, Object> map) {
                return new Builder(map);
            }
        };
    }
}
