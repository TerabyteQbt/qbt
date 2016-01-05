package qbt.manifest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import misc1.commons.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import qbt.tip.PackageTip;

public interface StringSerializer<T> {
    String toString(T t);
    T fromString(String s);

    public final StringSerializer<String> STRING = new StringSerializer<String>() {
        @Override
        public String toString(String t) {
            return t;
        }

        @Override
        public String fromString(String s) {
            return s;
        }
    };

    public final StringSerializer<Boolean> BOOLEAN = new StringSerializer<Boolean>() {
        @Override
        public String toString(Boolean t) {
            return t ? "true" : "false";
        }

        @Override
        public Boolean fromString(String s) {
            if(s.equals("true")) {
                return true;
            }
            if(s.equals("false")) {
                return false;
            }
            throw new IllegalArgumentException("Illegal value for boolean: " + s);
        }
    };

    public final StringSerializer<ImmutableSet<String>> V0_QBT_ENV = new StringSerializer<ImmutableSet<String>>() {
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

    public final StringSerializer<Maybe<String>> V0_PREFIX = new StringSerializer<Maybe<String>>() {
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

    public final StringSerializer<Pair<PackageTip, String>> VERIFY_DEP_KEY = new StringSerializer<Pair<PackageTip, String>>() {
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
