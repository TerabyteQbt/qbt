package qbt;

import com.google.common.base.Function;
import com.google.common.hash.HashCode;

public class VcsVersionDigest extends TypedDigest {
    public VcsVersionDigest(HashCode delegate) {
        super(delegate);
    }

    public static final Function<String, VcsVersionDigest> PARSE_FUNCTION = new Function<String, VcsVersionDigest>() {
        @Override
        public VcsVersionDigest apply(String input) {
            return new VcsVersionDigest(QbtHashUtils.parse(input));
        }
    };

    public static final Function<VcsVersionDigest, String> DEPARSE_FUNCTION = new Function<VcsVersionDigest, String>() {
        @Override
        public String apply(VcsVersionDigest input) {
            return input.getRawDigest().toString();
        }
    };
}
