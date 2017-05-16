package qbt.manifest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

abstract class JsonQbtManifestParser<M, B> implements QbtManifestParser<M> {
    private final QbtManifestVersion<M, B> version;

    public JsonQbtManifestParser(QbtManifestVersion<M, B> version) {
        this.version = version;
    }

    @Override
    public M parse(List<String> lines) {
        JsonElement json = new JsonParser().parse(Joiner.on('\n').join(lines));
        return version.build(serializer().fromJson(json));
    }

    @Override
    public ImmutableList<String> deparse(M manifest) {
        ImmutableList.Builder<String> b = ImmutableList.builder();
        JsonUtils.deparse(b, serializer().toJson(version.builder(manifest)));
        return b.build();
    }

    @Override
    public Pair<ImmutableList<Pair<String, String>>, ImmutableList<String>> deparse(String lhsName, M lhs, String mhsName, M mhs, String rhsName, M rhs) {
        JsonElement lhsJson = serializer().toJson(version.builder(lhs));
        JsonElement mhsJson = serializer().toJson(version.builder(mhs));
        JsonElement rhsJson = serializer().toJson(version.builder(rhs));
        JsonUtils.DeparseResultBuilder b = new JsonUtils.DeparseResultBuilder();
        JsonUtils.deparse(b, lhsName, lhsJson, mhsName, mhsJson, rhsName, rhsJson);
        return b.build();
    }

    protected abstract JsonSerializer<B> serializer();
}
