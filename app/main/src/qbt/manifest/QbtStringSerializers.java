package qbt.manifest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import misc1.commons.Maybe;
import misc1.commons.json.StringSerializer;
import org.apache.commons.lang3.tuple.Pair;
import qbt.tip.PackageTip;

public final class QbtStringSerializers {
    private QbtStringSerializers() {
        // no
    }

    public static final StringSerializer<ImmutableSet<String>> V0_QBT_ENV = new StringSerializer<ImmutableSet<String>>() {
        @Override
        public String toString(ImmutableSet<String> t) {
            List<String> valueOrdered = Lists.newArrayList(t);
            Collections.sort(valueOrdered);
            return Joiner.on(",").join(valueOrdered);
        }

        @Override
        public ImmutableSet<String> fromString(String s) {
            List<String> list = Lists.newArrayList(s.split(","));
            Collections.sort(list);
            return ImmutableSet.copyOf(list);
        }
    };

    public static final StringSerializer<Maybe<String>> V0_PREFIX = new StringSerializer<Maybe<String>>() {
        @Override
        public String toString(Maybe<String> prefix) {
            return prefix.get("NONE");
        }

        @Override
        public Maybe<String> fromString(String s) {
            if(s.equals("NONE")) {
                return Maybe.not();
            }
            return Maybe.of(s);
        }
    };

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
