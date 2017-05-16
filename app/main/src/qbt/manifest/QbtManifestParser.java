package qbt.manifest;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import qbt.manifest.current.QbtManifest;

public interface QbtManifestParser {
    QbtManifest parse(List<String> lines);
    ImmutableList<String> deparse(QbtManifest manifest);
    Pair<ImmutableList<Pair<String, String>>, ImmutableList<String>> deparse(String lhsName, QbtManifest lhs, String mhsName, QbtManifest mhs, String rhsName, QbtManifest rhs);
}
