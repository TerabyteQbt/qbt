package qbt.mains;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;

public class Version extends QbtCommand<Version.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Version.class);

    @QbtCommandName("version")
    public static interface Options extends QbtCommandOptions {
        public static final OptionsLibrary<Options> o = OptionsLibrary.of();
    }

    @Override
    public boolean isProgrammaticOutput() {
        return false;
    }

    @Override
    public Class<Options> getOptionsClass() {
        return Options.class;
    }

    @Override
    public HelpTier getHelpTier() {
        return HelpTier.COMMON;
    }

    @Override
    public String getDescription() {
        return "print version information and exit";
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws Exception {
        Enumeration<URL> e = Version.class.getClassLoader().getResources("META-INF/qbt/version");
        ImmutableSortedMap.Builder<Integer, String> versionStrings = ImmutableSortedMap.naturalOrder();
        while(e.hasMoreElements()) {
            final URL u = e.nextElement();
            final List<String> lines = new ByteSource() {
                @Override
                public InputStream openStream() throws IOException {
                    return u.openStream();
                }
            }.asCharSource(Charsets.UTF_8).readLines();
            for(String line : lines) {
                try {
                    String[] parts = line.split(" ", 2);
                    versionStrings.put(Integer.valueOf(parts[0]), parts[1]);
                }
                catch(NumberFormatException ex) {
                    throw new IllegalStateException("version information resource " + u.toString() + " has invalid format", ex);
                }
            }
        }
        versionStrings.build().forEach((Integer p, String v) -> LOGGER.info(v));
        return 0;
    }
}
