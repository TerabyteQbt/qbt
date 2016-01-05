package qbt.tip;

import com.google.common.base.Function;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import qbt.manifest.StringSerializer;

public abstract class AbstractTip<T extends AbstractTip<T>> {
    private static final Pattern PATTERN = Pattern.compile("^[0-9a-zA-Z._]*$");
    private static final Pattern TIP_PATTERN = Pattern.compile("^([0-9a-zA-Z._]*)\\^\\{([0-9a-zA-Z._]*)\\}$");

    private final Type<T> type;
    public final String name;
    public final String tip;

    protected AbstractTip(Type<T> type, String name, String tip) {
        this.type = type;
        this.name = name;
        this.tip = tip;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() ^ name.hashCode() ^ tip.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) {
            return false;
        }
        if(!getClass().equals(o.getClass())) {
            return false;
        }
        AbstractTip other = (AbstractTip) o;
        if(!name.equals(other.name)) {
            return false;
        }
        if(!tip.equals(other.tip)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if(tip.equals("HEAD")) {
            return name;
        }
        return name + "^{" + tip + "}";
    }

    public T replaceTip(String newTip) {
        return type.of(name, newTip);
    }

    public static abstract class Type<T extends AbstractTip> {
        public final Class<T> clazz;
        private final String desc;

        public Type(Class<T> clazz, String desc) {
            this.clazz = clazz;
            this.desc = desc;
        }

        public T parseLoose(String arg) {
            Matcher packageMatcher = PATTERN.matcher(arg);
            if(packageMatcher.matches()) {
                return of(arg, "HEAD");
            }

            Matcher packageTipMatcher = TIP_PATTERN.matcher(arg);
            if(packageTipMatcher.matches()) {
                return of(packageTipMatcher.group(1), packageTipMatcher.group(2));
            }

            return null;
        }

        public T parseRequire(String arg) {
            T t = parseLoose(arg);
            if(t == null) {
                throw new IllegalArgumentException("Neither a " + desc + " nor a " + desc + " tip: " + arg);
            }
            return t;
        }

        public final Comparator<T> COMPARATOR = new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                int r;

                r = a.name.compareTo(b.name);
                if(r != 0) {
                    return r;
                }

                r = a.tip.compareTo(b.tip);
                if(r != 0) {
                    return r;
                }

                return 0;
            }
        };

        public final Function<String, T> FROM_STRING = new Function<String, T>() {
            @Override
            public T apply(String input) {
                return parseRequire(input);
            }
        };

        public final StringSerializer<T> STRING_SERIALIZER = new StringSerializer<T>() {
            @Override
            public String toString(T t) {
                return t.toString();
            }

            @Override
            public T fromString(String s) {
                return parseRequire(s);
            }
        };

        public abstract T of(String name, String tip);
    }
}
