package qbt.manifest;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public interface QbtManifestParser<M> {
    M parse(List<String> lines);
    ImmutableList<String> deparse(M manifest);
    Pair<ImmutableList<Pair<String, String>>, ImmutableList<String>> deparse(String lhsName, M lhs, String mhsName, M mhs, String rhsName, M rhs);
}
