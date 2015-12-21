package qbt.options;

import java.io.IOException;
import qbt.manifest.QbtManifest;

public interface ManifestOptionsResult {
    QbtManifest parse() throws IOException;
    void deparse(QbtManifest manifest);
    boolean deparseConflict(String lhsName, QbtManifest lhs, String mhsName, QbtManifest mhs, String rhsName, QbtManifest rhs);
}
