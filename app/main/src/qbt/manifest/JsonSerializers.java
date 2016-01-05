package qbt.manifest;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import misc1.commons.ds.WrapperType;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.VcsVersionDigest;

public final class JsonSerializers {
    private JsonSerializers() {
        // nope
    }

    public static final JsonSerializer<String> STRING = new JsonSerializer<String>() {
        @Override
        public JsonElement toJson(String s) {
            return new JsonPrimitive(s);
        }

        @Override
        public String fromJson(JsonElement e) {
            return e.getAsString();
        }
    };

    public static final JsonSerializer<VcsVersionDigest> VCS_VERSION_DIGEST = new JsonSerializer<VcsVersionDigest>() {
        @Override
        public JsonElement toJson(VcsVersionDigest commit) {
            return new JsonPrimitive(VcsVersionDigest.DEPARSE_FUNCTION.apply(commit));
        }

        @Override
        public VcsVersionDigest fromJson(JsonElement e) {
            return VcsVersionDigest.PARSE_FUNCTION.apply(e.getAsString());
        }
    };

    public static final JsonSerializer<Pair<NormalDependencyType, String>> NORMAL_DEP_VALUE = new JsonSerializer<Pair<NormalDependencyType, String>>() {
        @Override
        public JsonElement toJson(Pair<NormalDependencyType, String> pair) {
            return new JsonPrimitive(pair.getLeft().getTag() + "," + pair.getRight());
        }

        @Override
        public Pair<NormalDependencyType, String> fromJson(JsonElement e) {
            String[] a = e.getAsString().split(",");
            if(a.length != 2) {
                throw new IllegalArgumentException();
            }
            return Pair.of(NormalDependencyType.fromTag(a[0]), a[1]);
        }
    };

    public static <V> JsonSerializer<V> forStringSerializer(final StringSerializer<V> delegate) {
        return new JsonSerializer<V>() {
            @Override
            public JsonElement toJson(V v) {
                return new JsonPrimitive(delegate.toString(v));
            }

            @Override
            public V fromJson(JsonElement e) {
                return delegate.fromString(e.getAsString());
            }
        };
    }

    public static <E extends Enum<E>> JsonSerializer<E> forEnum(final Class<E> clazz) {
        return new JsonSerializer<E>() {
            @Override
            public JsonElement toJson(E v) {
                return new JsonPrimitive(v.toString());
            }

            @Override
            public E fromJson(JsonElement e) {
                return Enum.valueOf(clazz, e.getAsString());
            }
        };
    }

    public static final JsonSerializer<ObjectUtils.Null> OU_NULL = new JsonSerializer<ObjectUtils.Null>() {
        @Override
        public JsonElement toJson(ObjectUtils.Null v) {
            return new JsonPrimitive(1);
        }

        @Override
        public ObjectUtils.Null fromJson(JsonElement e) {
            if(e.getAsInt() != 1) {
                throw new IllegalArgumentException();
            }
            return ObjectUtils.NULL;
        }
    };

    public static <W, D> JsonSerializer<W> wrapper(final WrapperType<W, D> type, final JsonSerializer<D> delegate) {
        return new JsonSerializer<W>() {
            @Override
            public JsonElement toJson(W w) {
                return delegate.toJson(type.unwrap(w));
            }

            @Override
            public W fromJson(JsonElement e) {
                return type.wrap(delegate.fromJson(e));
            }
        };
    }
}
