package qbt.manifest;

import misc1.commons.json.StringSerializer;
import org.apache.commons.lang3.tuple.Pair;
import qbt.tip.PackageTip;

public final class QbtStringSerializers {
    private QbtStringSerializers() {
        // no
    }

    public static final StringSerializer<Pair<PackageTip, String>> VERIFY_DEP_KEY = new StringSerializer<Pair<PackageTip, String>>() {
        @Override
        public String toString(Pair<PackageTip, String> pair) {
            return pair.getLeft() + "/" + pair.getRight();
        }

        @Override
        public Pair<PackageTip, String> fromString(String s) {
            int i = s.indexOf('/');
            if(i == -1) {
                throw new IllegalArgumentException();
            }
            return Pair.of(PackageTip.TYPE.parseRequire(s.substring(0, i)), s.substring(i + 1));
        }
    };
}
