package qbt.mains;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.io.Resources;
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
        StringBuilder version = new StringBuilder();
        version.append(System.getenv().getOrDefault("WG_PACKAGE_NAME", "unknown-package"));
        version.append("@");
        version.append(System.getenv().getOrDefault("WG_PACKAGE_CUMULATIVE_VERSION", "unknown-cumulative-version"));
        final String vanityName = System.getenv("WG_VANITY_NAME");
        if(vanityName != null) {
            version.append(" (" + vanityName + ")");
        }
        LOGGER.info(version.toString());

        Enumeration<URL> e = Version.class.getClassLoader().getResources("META-INF/qbt/version");
        ImmutableSortedSet.Builder<String> b = ImmutableSortedSet.naturalOrder();
        while(e.hasMoreElements()) {
            final List<String> lines = Resources.asCharSource(e.nextElement(), Charsets.UTF_8).readLines();
            for(String line : lines) {
                b.add("    " + line);
            }
        }
        LOGGER.info(Joiner.on("\n").join(b.build()));
        return 0;
    }
}
