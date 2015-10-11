package qbt.recursive.cv;

import com.google.common.hash.HashCode;
import qbt.QbtHashUtils;
import qbt.TypedDigest;

public final class CumulativeVersionDigest extends TypedDigest {
    public CumulativeVersionDigest(HashCode delegate) {
        super(delegate);
    }

    // This should get changed (randomly) every time we make changes to the
    // fundamental action of QBT in a way that should force a rebuild of the
    // world.
    public static final CumulativeVersionDigest QBT_VERSION = new CumulativeVersionDigest(QbtHashUtils.parse("3eb9bfa6ca61dd44af98cea8ed7640a838606418"));
}
