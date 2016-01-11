package qbt.options;

import java.nio.file.Path;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;
import org.apache.commons.lang3.tuple.Pair;
import qbt.QbtUtils;
import qbt.config.QbtConfig;

public class ConfigOptionsDelegate<O> implements OptionsDelegate<O> {
    private final OptionsLibrary<O> o = OptionsLibrary.of();
    public final OptionsFragment<O, String> file = o.oneArg("config").transform(o.singleton(null)).helpDesc("QBT config file");

    public QbtConfig getConfig(OptionsResults<? extends O> options) {
        return getPair(options).getRight();
    }

    public Pair<Path, QbtConfig> getPair(OptionsResults<? extends O> options) {
        Path configFile = QbtUtils.findInMeta("qbt-config", options.get(file));
        return Pair.of(configFile, QbtConfig.parse(configFile));
    }
}
