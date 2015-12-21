package qbt.options;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.apache.commons.lang3.tuple.Pair;
import qbt.manifest.QbtManifest;

public interface ManifestOptionsResult {
    QbtManifest parse() throws IOException;
    void deparse(QbtManifest manifest);
    ImmutableList<Pair<String, String>> deparseConflict(String lhsName, QbtManifest lhs, String mhsName, QbtManifest mhs, String rhsName, QbtManifest rhs);
}
