package qbt.manifest;

import java.util.List;
import qbt.manifest.current.QbtManifest;

public class QbtManifestVersions {
    static final V3QbtManifestVersion V3 = new V3QbtManifestVersion();

    public static LegacyQbtManifest<?, ?> parseLegacy(List<String> lines) {
        final QbtManifestVersion<?, ?> version = V3;
        return new Object() {
            public <M, B> LegacyQbtManifest<M, B> run(QbtManifestVersion<M, B> version) {
                return new LegacyQbtManifest<M, B>(version, version.parser().parse(lines));
            }
        }.run(version);
    }

    public static QbtManifest parse(List<String> lines) {
        return parseLegacy(lines).current();
    }

    public static LegacyQbtManifest<?, ?> toLegacy(QbtManifest manifest) {
        return new LegacyQbtManifest<QbtManifest, QbtManifest.Builder>(V3, manifest);
    }
}
