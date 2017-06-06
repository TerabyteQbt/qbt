package qbt.manifest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.List;
import misc1.commons.json.JsonSerializer;
import org.apache.commons.lang3.tuple.Pair;
import qbt.manifest.current.QbtManifest;

public abstract class JsonQbtManifestParser implements QbtManifestParser {
    @Override
    public QbtManifest parse(List<String> lines) {
        JsonElement json = new JsonParser().parse(Joiner.on('\n').join(lines));
        return serializer().fromJson(json);
    }

    @Override
    public ImmutableList<String> deparse(QbtManifest manifest) {
        ImmutableList.Builder<String> b = ImmutableList.builder();
        JsonUtils.deparse(b, serializer().toJson(manifest));
        return b.build();
    }

    @Override
    public Pair<ImmutableList<Pair<String, String>>, ImmutableList<String>> deparse(String lhsName, QbtManifest lhs, String mhsName, QbtManifest mhs, String rhsName, QbtManifest rhs) {
        JsonElement lhsJson = serializer().toJson(lhs);
        JsonElement mhsJson = serializer().toJson(mhs);
        JsonElement rhsJson = serializer().toJson(rhs);
        JsonUtils.DeparseResultBuilder b = new JsonUtils.DeparseResultBuilder();
        JsonUtils.deparse(b, lhsName, lhsJson, mhsName, mhsJson, rhsName, rhsJson);
        return b.build();
    }

    protected abstract JsonSerializer<QbtManifest> serializer();
}
