package qbt.manifest;

import com.google.common.collect.ImmutableMap;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.MapStruct;
import misc1.commons.ds.MapStructBuilder;
import misc1.commons.ds.SimpleMapStructType;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;

public final class PackageManifestNormalDeps extends MapStruct<PackageManifestNormalDeps, PackageManifestNormalDeps.Builder, String, Pair<NormalDependencyType, String>, Pair<NormalDependencyType, String>> {
    public final ImmutableMap<String, Pair<NormalDependencyType, String>> map;

    private PackageManifestNormalDeps(ImmutableMap<String, Pair<NormalDependencyType, String>> map) {
        super(TYPE, map);
        this.map = map;
    }

    public static class Builder extends MapStructBuilder<PackageManifestNormalDeps, Builder, String, Pair<NormalDependencyType, String>, Pair<NormalDependencyType, String>> {
        public Builder(ImmutableSalvagingMap<String, Pair<NormalDependencyType, String>> map) {
            super(TYPE, map);
        }
    }

    public static final SimpleMapStructType<PackageManifestNormalDeps, Builder, String, Pair<NormalDependencyType, String>> TYPE = new SimpleMapStructType<PackageManifestNormalDeps, Builder, String, Pair<NormalDependencyType, String>>() {
        @Override
        protected PackageManifestNormalDeps create(ImmutableMap<String, Pair<NormalDependencyType, String>> map) {
            return new PackageManifestNormalDeps(map);
        }

        @Override
        protected Builder createBuilder(ImmutableSalvagingMap<String, Pair<NormalDependencyType, String>> map) {
            return new Builder(map);
        }
    };
}
