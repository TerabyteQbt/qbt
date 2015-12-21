package qbt.manifest;

import com.google.common.collect.ImmutableMap;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.MapStruct;
import misc1.commons.ds.MapStructBuilder;
import misc1.commons.ds.SimpleMapStructType;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import qbt.tip.PackageTip;

public final class PackageManifestVerifyDeps extends MapStruct<PackageManifestVerifyDeps, PackageManifestVerifyDeps.Builder, Pair<PackageTip, String>, ObjectUtils.Null, ObjectUtils.Null> {
    public final ImmutableMap<Pair<PackageTip, String>, ObjectUtils.Null> map;

    private PackageManifestVerifyDeps(ImmutableMap<Pair<PackageTip, String>, ObjectUtils.Null> map) {
        super(TYPE, map);
        this.map = map;
    }

    public static class Builder extends MapStructBuilder<PackageManifestVerifyDeps, Builder, Pair<PackageTip, String>, ObjectUtils.Null, ObjectUtils.Null> {
        public Builder(ImmutableSalvagingMap<Pair<PackageTip, String>, ObjectUtils.Null> map) {
            super(TYPE, map);
        }
    }

    public static final SimpleMapStructType<PackageManifestVerifyDeps, Builder, Pair<PackageTip, String>, ObjectUtils.Null> TYPE = new SimpleMapStructType<PackageManifestVerifyDeps, Builder, Pair<PackageTip, String>, ObjectUtils.Null>() {
        @Override
        protected PackageManifestVerifyDeps create(ImmutableMap<Pair<PackageTip, String>, ObjectUtils.Null> map) {
            return new PackageManifestVerifyDeps(map);
        }

        @Override
        protected Builder createBuilder(ImmutableSalvagingMap<Pair<PackageTip, String>, ObjectUtils.Null> map) {
            return new Builder(map);
        }
    };
}
