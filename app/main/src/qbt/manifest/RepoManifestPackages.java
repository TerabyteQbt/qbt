package qbt.manifest;

import com.google.common.collect.ImmutableMap;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.MapStruct;
import misc1.commons.ds.MapStructBuilder;
import misc1.commons.ds.MapStructType;

public final class RepoManifestPackages extends MapStruct<RepoManifestPackages, RepoManifestPackages.Builder, String, PackageManifest, PackageManifest.Builder> {
    public final ImmutableMap<String, PackageManifest> packages;

    private RepoManifestPackages(ImmutableMap<String, PackageManifest> packages) {
        super(TYPE, packages);
        this.packages = packages;
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
            return vb.build();
        }

        @Override
        protected PackageManifest.Builder toBuilder(PackageManifest vs) {
            return vs.builder();
        }
    };
}
