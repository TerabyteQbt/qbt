package qbt;

import com.google.common.base.Function;
import com.google.common.hash.HashCode;

public class VcsVersionDigest extends TypedDigest {
    public VcsVersionDigest(HashCode delegate) {
        super(delegate);
    }

    public static final Function<String, VcsVersionDigest> PARSE_FUNCTION = (input) -> new VcsVersionDigest(QbtHashUtils.parse(input));

    public static final Function<VcsVersionDigest, String> DEPARSE_FUNCTION = (input) -> input.getRawDigest().toString();
}
