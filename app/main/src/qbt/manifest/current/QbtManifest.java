package qbt.manifest.current;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.MapStruct;
import misc1.commons.ds.MapStructBuilder;
import misc1.commons.ds.MapStructType;
import misc1.commons.merge.Merge;
import qbt.manifest.JsonSerializer;
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
    };

    public static final JsonSerializer<Builder> SERIALIZER = new JsonSerializer<Builder>() {
        @Override
        public JsonElement toJson(Builder b) {
            JsonObject r = new JsonObject();
            for(Map.Entry<RepoTip, RepoManifest.Builder> e : b.map.entries()) {
                r.add(RepoTip.TYPE.STRING_SERIALIZER.toString(e.getKey()), RepoManifest.SERIALIZER.toJson(e.getValue()));
            }
            return r;
        }

        @Override
        public Builder fromJson(JsonElement e) {
            Builder b = TYPE.builder();
            for(Map.Entry<String, JsonElement> e2 : e.getAsJsonObject().entrySet()) {
                b = b.with(RepoTip.TYPE.STRING_SERIALIZER.fromString(e2.getKey()), RepoManifest.SERIALIZER.fromJson(e2.getValue()));
            }
            return b;
        }
    };
}
