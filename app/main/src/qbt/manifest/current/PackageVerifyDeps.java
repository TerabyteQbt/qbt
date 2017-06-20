package qbt.manifest.current;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.MapStruct;
import misc1.commons.ds.MapStructBuilder;
import misc1.commons.ds.MapStructType;
import misc1.commons.json.JsonSerializer;
import misc1.commons.json.JsonSerializers;
import misc1.commons.json.StringSerializer;
import misc1.commons.merge.Merge;
import misc1.commons.merge.Merges;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import qbt.manifest.QbtStringSerializers;
import qbt.tip.PackageTip;

public final class PackageVerifyDeps extends MapStruct<PackageVerifyDeps, PackageVerifyDeps.Builder, Pair<PackageTip, String>, ObjectUtils.Null, ObjectUtils.Null> {
    private PackageVerifyDeps(ImmutableMap<Pair<PackageTip, String>, ObjectUtils.Null> map) {
        super(TYPE, map);
    }

    public static class Builder extends MapStructBuilder<PackageVerifyDeps, Builder, Pair<PackageTip, String>, ObjectUtils.Null, ObjectUtils.Null> {
        public Builder(ImmutableSalvagingMap<Pair<PackageTip, String>, ObjectUtils.Null> map) {
            super(TYPE, map);
        }
    }

    public static final MapStructType<PackageVerifyDeps, Builder, Pair<PackageTip, String>, ObjectUtils.Null, ObjectUtils.Null> TYPE = new MapStructType<PackageVerifyDeps, Builder, Pair<PackageTip, String>, ObjectUtils.Null, ObjectUtils.Null>() {
        @Override
        protected PackageVerifyDeps create(ImmutableMap<Pair<PackageTip, String>, ObjectUtils.Null> map) {
            return new PackageVerifyDeps(map);
        }

        @Override
        protected Builder createBuilder(ImmutableSalvagingMap<Pair<PackageTip, String>, ObjectUtils.Null> map) {
            return new Builder(map);
        }

        @Override
        protected ObjectUtils.Null toStruct(ObjectUtils.Null vb) {
            return vb;
        }

        @Override
        protected ObjectUtils.Null toBuilder(ObjectUtils.Null vs) {
            return vs;
        }

        @Override
        protected Merge<ObjectUtils.Null> mergeValue() {
            return Merges.<ObjectUtils.Null>trivial();
        }

        @Override
        protected Optional<StringSerializer<Pair<PackageTip, String>>> keySerializer() {
            return Optional.of(QbtStringSerializers.VERIFY_DEP_KEY);
        }

        @Override
        protected Optional<JsonSerializer<ObjectUtils.Null>> valueSerializer() {
            return Optional.of(JsonSerializers.OU_NULL);
        }
    };
}
