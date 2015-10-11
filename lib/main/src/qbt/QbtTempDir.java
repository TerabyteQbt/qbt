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
