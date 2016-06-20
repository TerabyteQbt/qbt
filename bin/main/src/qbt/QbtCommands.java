//   Copyright 2016 Keith Amling
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
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
