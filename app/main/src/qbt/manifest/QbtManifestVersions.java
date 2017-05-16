package qbt.manifest;

import java.util.List;
import qbt.manifest.current.QbtManifest;

public class QbtManifestVersions {
    static final V3QbtManifestVersion V3 = new V3QbtManifestVersion();

    public static LegacyQbtManifest<?, ?> parseLegacy(List<String> lines) {
        final QbtManifestVersion<?, ?> version;
        if(lines.isEmpty()) {
            throw new IllegalArgumentException();
        }
        else {
            String line0 = lines.get(0);
            if(!line0.startsWith("@")) {
                throw new IllegalArgumentException();
            }
            else {
                lines = lines.subList(1, lines.size());
                int n = Integer.parseInt(line0.substring(1), 10);
                if(n != 3) {
                    throw new IllegalArgumentException();
                }
                version = V3;
            }
        }
        final List<String> linesFinal = lines;
        return new Object() {
            public <M, B> LegacyQbtManifest<M, B> run(QbtManifestVersion<M, B> version) {
                return new LegacyQbtManifest<M, B>(version, version.parser().parse(linesFinal));
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
