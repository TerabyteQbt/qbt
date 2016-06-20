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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import misc1.commons.ExceptionUtils;

public class QbtTempDir implements Closeable {
    public final Path path;

    public QbtTempDir() {
        try {
            this.path = Files.createTempDirectory(null);
        }
        catch(IOException e) {
            throw ExceptionUtils.commute(e);
        }
    }

    public Path resolve(String other) {
        return path.resolve(other);
    }

    @Override
    public void close() {
        QbtUtils.deleteRecursively(path, false);
    }
}
