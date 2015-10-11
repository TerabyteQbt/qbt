package qbt.map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import misc1.commons.options.NamedStringListArgumentOptionsFragment;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsException;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;

public class CumulativeVersionComputerOptionsDelegate<O> implements OptionsDelegate<O> {
    public final OptionsFragment<O, ?, ImmutableList<String>> qbtEnv = new NamedStringListArgumentOptionsFragment<O>(ImmutableList.of("--qbtEnv"), "Qbt environment variables");

    public CumulativeVersionComputerOptionsResult getResults(OptionsResults<? extends O> options) {
        ImmutableMap.Builder<String, String> qbtEnvBuilder = ImmutableMap.builder();
        for(String s : options.get(qbtEnv)) {
            int i = s.indexOf('=');
            if(i == -1) {
                throw new OptionsException("Qbt environment argument did not have name and value: " + s);
            }
            qbtEnvBuilder.put(s.substring(0, i), s.substring(i + 1));
        }
        return new CumulativeVersionComputerOptionsResult(qbtEnvBuilder.build());
    }
}
