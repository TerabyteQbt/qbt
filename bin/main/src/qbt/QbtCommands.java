package qbt;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import misc1.commons.ExceptionUtils;

public class QbtCommands {
    private QbtCommands() {
        // nope
    }

    private static Map<String, QbtCommand<?>> loadCommands() {
        try {
            ImmutableMap.Builder<String, QbtCommand<?>> b = ImmutableMap.builder();

            Enumeration<URL> e = QbtCommands.class.getClassLoader().getResources("META-INF/qbt/commands");
            while(e.hasMoreElements()) {
                final URL u = e.nextElement();
                List<String> lines = new ByteSource() {
                    @Override
                    public InputStream openStream() throws IOException {
                        return u.openStream();
                    }
                }.asCharSource(Charsets.UTF_8).readLines();
                for(String line : lines) {
                    QbtCommand<?> instance = (QbtCommand<?>) Class.forName(line).newInstance();
                    b.put(getName(instance), instance);
                }
            }

            return b.build();
        }
        catch(Exception e) {
            throw ExceptionUtils.commute(e);
        }
    }

    public static Map<String, QbtCommand<?>> getCommands() {
        return Loader.INSTANCE.COMMANDS;
    }

    public static String getName(QbtCommand<?> instance) {
        QbtCommandName a = instance.getOptionsClass().getAnnotation(QbtCommandName.class);
        if(a == null) {
            throw new IllegalStateException("No @QbtCommandName on " + instance.getClass().getName() + "/" + instance.getOptionsClass().getName());
        }
        return a.value();
    }

    private enum Loader {
        INSTANCE;

        private final Map<String, QbtCommand<?>> COMMANDS;

        private Loader() {
            this.COMMANDS = loadCommands();
        }
    }
}
