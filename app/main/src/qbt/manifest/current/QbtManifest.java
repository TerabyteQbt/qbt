package qbt.manifest.current;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.MapStruct;
import misc1.commons.ds.MapStructBuilder;
import misc1.commons.ds.MapStructType;
import misc1.commons.json.JsonSerializer;
import misc1.commons.json.StringSerializer;
import misc1.commons.merge.Merge;
import qbt.manifest.QbtManifestUtils;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public final class QbtManifest extends MapStruct<QbtManifest, QbtManifest.Builder, RepoTip, RepoManifest, RepoManifest.Builder> {
    public final ImmutableMap<RepoTip, RepoManifest> repos;
    public final ImmutableMap<PackageTip, RepoTip> packageToRepo;

    private QbtManifest(ImmutableMap<RepoTip, RepoManifest> map) {
        super(TYPE, map);

        this.repos = map;
        this.packageToRepo = QbtManifestUtils.invertReposMap(repos);
    }

    public static class Builder extends MapStructBuilder<QbtManifest, Builder, RepoTip, RepoManifest, RepoManifest.Builder> {
        public Builder(ImmutableSalvagingMap<RepoTip, RepoManifest.Builder> map) {
            super(TYPE, map);
        }
    }

    public static final MapStructType<QbtManifest, Builder, RepoTip, RepoManifest, RepoManifest.Builder> TYPE = new MapStructType<QbtManifest, Builder, RepoTip, RepoManifest, RepoManifest.Builder>() {
        @Override
        protected QbtManifest create(ImmutableMap<RepoTip, RepoManifest> map) {
            return new QbtManifest(map);
        }

        @Override
        protected Builder createBuilder(ImmutableSalvagingMap<RepoTip, RepoManifest.Builder> map) {
            return new Builder(map);
        }

        @Override
        protected RepoManifest toStruct(RepoManifest.Builder vb) {
            return (vb).build();
        }

        @Override
        protected RepoManifest.Builder toBuilder(RepoManifest vs) {
            return (vs).builder();
        }

        @Override
        protected Merge<RepoManifest> mergeValue() {
            return RepoManifest.TYPE.merge();
        }

        @Override
        protected Optional<StringSerializer<RepoTip>> keySerializer() {
            return Optional.of(RepoTip.TYPE.STRING_SERIALIZER);
        }

        @Override
        protected Optional<JsonSerializer<RepoManifest.Builder>> valueSerializer() {
            return Optional.of(RepoManifest.TYPE.serializerB());
        }
    };

    // this is both (a) an optimization to only construct it once, and (b) an
    // important high-level check that this serializer is defined all the way
    // down.
    public static final JsonSerializer<Builder> SERIALIZER = TYPE.serializerB();
}
