package qbt.manifest;

import com.google.common.collect.ImmutableList;
import java.util.List;
import qbt.manifest.current.QbtManifest;

public class QbtManifestVersions {
    private static class Internals {
        public final ImmutableList<QbtManifestVersion<?, ?>> list;

        public Internals(ImmutableList<QbtManifestVersion<?, ?>> list) {
            this.list = list;

            for(int i = 0; i < list.size(); ++i) {
                if(list.get(i).version != i) {
                    throw new IllegalArgumentException();
                }
                if(i + 1 < list.size()) {
                    QbtManifestVersion<?, ?> before = list.get(i);
                    QbtManifestVersion<?, ?> after = list.get(i + 1);
                    if(!(before instanceof QbtManifestUpgradeableVersion)) {
                        throw new IllegalArgumentException();
                    }
                    if(!((QbtManifestUpgradeableVersion<?, ?, ?>) before).upgradeManifestClass.equals(after.manifestClass)) {
                        throw new IllegalArgumentException();
                    }
                }
            }
        }
    }

    static final V0QbtManifestVersion V0;
    static final V1QbtManifestVersion V1;
    static final V2QbtManifestVersion V2;
    static final V3QbtManifestVersion V3;
    private static final Internals INTERNALS;
    static {
        ImmutableList.Builder<QbtManifestVersion<?, ?>> b = ImmutableList.builder();

        b.add(V3 = new V3QbtManifestVersion());
        b.add(V2 = new V2QbtManifestVersion(V3));
        b.add(V1 = new V1QbtManifestVersion(V2));
        b.add(V0 = new V0QbtManifestVersion(V1));

        INTERNALS = new Internals(b.build().reverse());
    }

    public static LegacyQbtManifest<?, ?> parseLegacy(List<String> lines) {
        final QbtManifestVersion<?, ?> version;
        if(lines.isEmpty()) {
            version = V0;
        }
        else {
            String line0 = lines.get(0);
            if(!line0.startsWith("@")) {
                version = V0;
            }
            else {
                lines = lines.subList(1, lines.size());
                version = INTERNALS.list.get(Integer.parseInt(line0.substring(1), 10));
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
