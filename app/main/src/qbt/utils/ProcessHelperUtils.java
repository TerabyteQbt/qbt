package qbt.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public class ProcessHelperUtils {
    private ProcessHelperUtils() {
        // no
    }

    public static void runQuiet(Path dir, String... cmd) {
        runQuiet(dir, ImmutableMap.<String, String>of(), cmd);
    }

    public static void runQuiet(Path dir, Map<String, String> env, String... cmd) {
        ProcessHelper p = new ProcessHelper(dir, cmd);
        p = p.combineError();
        for(Map.Entry<String, String> e : env.entrySet()) {
            p = p.putEnv(e.getKey(), e.getValue());
        }
        Pair<ImmutableList<String>, Integer> r = p.completeLinesAndExitCode();
        int exit = r.getRight();
        if(exit != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(p.getDescription() + " failed: " + exit);
            for(String line : r.getLeft()) {
                sb.append("\n");
                sb.append(line);
            }
            throw new RuntimeException(sb.toString());
        }
    }
}
