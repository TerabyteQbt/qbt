package qbt;

import java.io.Closeable;
import java.nio.file.Path;

public interface PackageDirectory extends Closeable {
    public Path getDir();

    @Override
    public void close();
}
