package qbt.manifest.current;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.MapStruct;
import misc1.commons.ds.MapStructBuilder;
import misc1.commons.ds.MapStructType;
import misc1.commons.json.JsonSerializer;
import misc1.commons.json.StringSerializer;
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

        @Override
        protected Optional<StringSerializer<String>> keySerializer() {
            return Optional.of(StringSerializers.STRING);
        }

        @Override
        protected Optional<JsonSerializer<PackageManifest.Builder>> valueSerializer() {
            return Optional.of(PackageManifest.TYPE.serializerB());
        }
    };
}
