package qbt.manifest;

import com.google.common.collect.ImmutableMap;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.MapStruct;
import misc1.commons.ds.MapStructBuilder;
import misc1.commons.ds.SimpleMapStructType;
import qbt.tip.PackageTip;

public final class PackageManifestReplaceDeps extends MapStruct<PackageManifestReplaceDeps, PackageManifestReplaceDeps.Builder, PackageTip, String, String> {
    public final ImmutableMap<PackageTip, String> map;

    private PackageManifestReplaceDeps(ImmutableMap<PackageTip, String> map) {
        super(TYPE, map);
        this.map = map;
    }

    public static class Builder extends MapStructBuilder<PackageManifestReplaceDeps, Builder, PackageTip, String, String> {
        public Builder(ImmutableSalvagingMap<PackageTip, String> map) {
            super(TYPE, map);
        }
    }

    public static final SimpleMapStructType<PackageManifestReplaceDeps, Builder, PackageTip, String> TYPE = new SimpleMapStructType<PackageManifestReplaceDeps, Builder, PackageTip, String>() {
        @Override
        protected PackageManifestReplaceDeps create(ImmutableMap<PackageTip, String> map) {
            return new PackageManifestReplaceDeps(map);
        }

        @Override
        protected Builder createBuilder(ImmutableSalvagingMap<PackageTip, String> map) {
            return new Builder(map);
        }
    };
}
