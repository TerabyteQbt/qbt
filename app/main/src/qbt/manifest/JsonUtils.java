package qbt.manifest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import org.apache.commons.lang3.tuple.Pair;

public final class JsonUtils {
    private JsonUtils() {
        // nope
    }

    public static ImmutableList<String> deparse(JsonElement e) {
        ImmutableList.Builder<String> b = ImmutableList.builder();
        deparse(b, e);
        return b.build();
    }

    public static void deparse(ImmutableList.Builder<String> b, JsonElement e) {
        deparse(b, "", true, null, e);
    }

    private static void deparse(ImmutableList.Builder<String> b, String indent, boolean isFinal, String label, JsonElement e) {
        if(e.isJsonObject()) {
            deparse(b, indent, isFinal, label, e.getAsJsonObject());
            return;
        }
        if(e.isJsonPrimitive()) {
            deparse(b, indent, isFinal, label, e.getAsJsonPrimitive());
            return;
        }
        throw new IllegalArgumentException();
    }

    private static void deparse(ImmutableList.Builder<String> b, String indent, boolean isFinal, String label, JsonObject e) {
        TreeSet<String> keys = Sets.newTreeSet();
        for(Map.Entry<String, JsonElement> e2 : e.entrySet()) {
            keys.add(e2.getKey());
        }
        b.add(indent + keyPrefix(label) + "{");
        Iterator<String> i = keys.iterator();
        while(i.hasNext()) {
            String key = i.next();
            deparse(b, indent + "    ", !i.hasNext(), key, e.get(key));
        }
        b.add(indent + "}" + commaSuffix(isFinal));
    }

    private static void deparse(ImmutableList.Builder<String> b, String indent, boolean isFinal, String label, JsonPrimitive e) {
        b.add(indent + keyPrefix(label) + deparse(e) + commaSuffix(isFinal));
    }

    public static class DeparseResultBuilder {
        public final ImmutableList.Builder<Pair<String, String>> conflicts = ImmutableList.builder();
        public final ImmutableList.Builder<String> lines = ImmutableList.builder();

        public void add(String s) {
            lines.add(s);
        }

        public void add(String path, String type) {
            conflicts.add(Pair.of(path, type));
        }

        public Pair<ImmutableList<Pair<String, String>>, ImmutableList<String>> build() {
            return Pair.of(conflicts.build(), lines.build());
        }
    }

    public static Pair<ImmutableList<Pair<String, String>>, ImmutableList<String>> deparse(String lhsName, JsonElement lhs, String mhsName, JsonElement mhs, String rhsName, JsonElement rhs) {
        DeparseResultBuilder b = new DeparseResultBuilder();
        deparse(b, lhsName, lhs, mhsName, mhs, rhsName, rhs);
        return b.build();
    }

    public static void deparse(DeparseResultBuilder b, String lhsName, JsonElement lhs, String mhsName, JsonElement mhs, String rhsName, JsonElement rhs) {
        deparse(b, "", "", true, null, lhsName, lhs, mhsName, mhs, rhsName, rhs);
    }

    private static void deparse(DeparseResultBuilder b, String indent, String path, boolean isFinal, String label, String lhsName, JsonElement lhs, String mhsName, JsonElement mhs, String rhsName, JsonElement rhs) {
        if(lhs.isJsonObject() && mhs.isJsonObject() && rhs.isJsonObject()) {
            deparse(b, indent, path, isFinal, label, lhsName, lhs.getAsJsonObject(), mhsName, mhs.getAsJsonObject(), rhsName, rhs.getAsJsonObject());
            return;
        }
        if(lhs.isJsonPrimitive() && mhs.isJsonPrimitive() && rhs.isJsonPrimitive()) {
            deparse(b, indent, path, isFinal, label, lhsName, lhs.getAsJsonPrimitive(), mhsName, mhs.getAsJsonPrimitive(), rhsName, rhs.getAsJsonPrimitive());
            return;
        }
        throw new IllegalArgumentException();
    }

    private static void deparse(DeparseResultBuilder b, String indent, String path, boolean isFinal, String label, String lhsName, JsonObject lhs, String mhsName, JsonObject mhs, String rhsName, JsonObject rhs) {
        TreeSet<String> keys = Sets.newTreeSet();
        for(JsonObject o : ImmutableList.of(lhs, mhs, rhs)) {
            for(Map.Entry<String, JsonElement> e : o.entrySet()) {
                keys.add(e.getKey());
            }
        }
        b.add(indent + keyPrefix(label) + "{");
        Iterator<String> i = keys.iterator();
        while(i.hasNext()) {
            String key = i.next();
            JsonElement lhsE = lhs.get(key);
            JsonElement mhsE = mhs.get(key);
            JsonElement rhsE = rhs.get(key);
            String indent2 = indent + "    ";
            String path2 = path.equals("") ? key : (path + "/" + key);
            boolean isFinal2 = !i.hasNext();

            if(lhsE == null || mhsE == null || rhsE == null) {
                if(lhsE == null) {
                    b.add(path2, "DELETE/EDIT");
                }
                if(mhsE == null) {
                    b.add(path2, "ADD/ADD");
                }
                if(rhsE == null) {
                    b.add(path2, "EDIT/DELETE");
                }

                b.add("<<<<<<< " + lhsName);
                if(lhsE != null) {
                    deparse(b, indent2, path2, isFinal2, key, lhsName, lhsE, lhsName, lhsE, lhsName, lhsE);
                }
                b.add("||||||| " + mhsName);
                if(mhsE != null) {
                    deparse(b, indent2, path2, isFinal2, key, mhsName, mhsE, mhsName, mhsE, mhsName, mhsE);
                }
                b.add("=======");
                if(rhsE != null) {
                    deparse(b, indent2, path2, isFinal2, key, rhsName, rhsE, rhsName, rhsE, rhsName, rhsE);
                }
                b.add(">>>>>>> " + rhsName);
                continue;
            }
            deparse(b, indent2, path2, isFinal2, key, lhsName, lhsE, mhsName, mhsE, rhsName, rhsE);
        }
        b.add(indent + "}" + commaSuffix(isFinal));
    }

    private static void deparse(DeparseResultBuilder b, String indent, String path, boolean isFinal, String label, String lhsName, JsonPrimitive lhs, String mhsName, JsonPrimitive mhs, String rhsName, JsonPrimitive rhs) {
        String prefix = indent + keyPrefix(label);
        if(lhs.equals(mhs) && mhs.equals(rhs)) {
            b.add(prefix + deparse(mhs) + commaSuffix(isFinal));
            return;
        }
        b.add(path, "EDIT/EDIT");
        b.add("<<<<<<< " + lhsName);
        b.add(prefix + deparse(lhs) + commaSuffix(isFinal));
        b.add("||||||| " + mhsName);
        b.add(prefix + deparse(mhs) + commaSuffix(isFinal));
        b.add("=======");
        b.add(prefix + deparse(rhs) + commaSuffix(isFinal));
        b.add(">>>>>>> " + rhsName);
    }

    private static String deparse(String s) {
        return deparse(new JsonPrimitive(s));
    }

    private static String deparse(JsonPrimitive e) {
        return new Gson().toJson(e);
    }

    private static String keyPrefix(String label) {
        return label == null ? "" : (deparse(label) + ": ");
    }

    private static String commaSuffix(boolean isFinal) {
        return isFinal ? "" : ",";
    }
}
