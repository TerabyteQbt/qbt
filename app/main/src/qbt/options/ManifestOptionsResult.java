package qbt.options;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.apache.commons.lang3.tuple.Pair;
import qbt.manifest.LegacyQbtManifest;
import qbt.manifest.QbtManifestVersion;
import qbt.manifest.current.QbtManifest;

public interface ManifestOptionsResult {
    LegacyQbtManifest<?, ?> parseLegacy() throws IOException;
    QbtManifest parse() throws IOException;
    void deparse(QbtManifest manifest);
    void deparse(LegacyQbtManifest<?, ?> manifest);
    <M, B> ImmutableList<Pair<String, String>> deparseConflict(QbtManifestVersion<M, B> version, String lhsName, M lhs, String mhsName, M mhs, String rhsName, M rhs);
}
