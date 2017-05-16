package qbt.options;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.apache.commons.lang3.tuple.Pair;
import qbt.manifest.QbtManifestParser;
import qbt.manifest.current.QbtManifest;

public interface ManifestOptionsResult {
    QbtManifest parse(QbtManifestParser parser) throws IOException;
    void deparse(QbtManifestParser parser, QbtManifest manifest);
    ImmutableList<Pair<String, String>> deparseConflict(QbtManifestParser parser, String lhsName, QbtManifest lhs, String mhsName, QbtManifest mhs, String rhsName, QbtManifest rhs);
}
