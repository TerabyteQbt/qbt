package qbt.map;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class CumulativeVersionComputerOptionsResult {
    public final Map<String, String> qbtEnv;

    public CumulativeVersionComputerOptionsResult(Map<String, String> qbtEnv) {
        this.qbtEnv = ImmutableMap.copyOf(qbtEnv);
    }
}
