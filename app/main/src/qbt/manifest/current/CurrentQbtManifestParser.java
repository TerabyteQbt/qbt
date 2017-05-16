package qbt.manifest.current;

import com.google.gson.JsonElement;
import qbt.manifest.JsonQbtManifestParser;
import qbt.manifest.JsonSerializer;

public final class CurrentQbtManifestParser extends JsonQbtManifestParser {
    @Override
    protected JsonSerializer<QbtManifest> serializer() {
        return new JsonSerializer<QbtManifest>() {
            @Override
            public JsonElement toJson(QbtManifest manifest) {
                return QbtManifest.SERIALIZER.toJson(manifest.builder());
            }

            @Override
            public QbtManifest fromJson(JsonElement e) {
                return QbtManifest.SERIALIZER.fromJson(e).build();
            }
        };
    }
}
