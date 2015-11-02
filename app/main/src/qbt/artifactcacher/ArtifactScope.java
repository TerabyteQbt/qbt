package qbt.artifactcacher;

import com.google.common.io.ByteStreams;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import misc1.commons.ExceptionUtils;
import misc1.commons.Maybe;
import misc1.commons.resources.FreeScope;
import qbt.QbtHashUtils;
import qbt.QbtTempDir;
import qbt.QbtUtils;
import qbt.utils.TarballUtils;

public class ArtifactScope implements Closeable {
    QbtTempDir root = new QbtTempDir();

    private synchronized void checkOpen() {
        if(root == null) {
            throw new IllegalStateException("ArtifactScope used after closed?");
        }
    }

    public synchronized ArtifactReference copyFile(Path file, boolean allowMiss) {
        checkOpen();
        Path copy = root.resolve(QbtHashUtils.random() + ".tar.gz");
        try {
            try(InputStream is = QbtUtils.openRead(file); OutputStream os = QbtUtils.openWrite(copy)) {
                ByteStreams.copy(is, os);
            }
        }
        catch(FileNotFoundException e) {
            if(allowMiss) {
                // nope
                return null;
            }
            throw ExceptionUtils.commute(e);
        }
        catch(IOException e) {
            throw ExceptionUtils.commute(e);
        }
        return artifactReference(copy);
    }

    public synchronized ArtifactReference copyDirectory(Path dir) {
        checkOpen();
        Path copy = root.resolve(QbtHashUtils.random() + ".tar.gz");
        TarballUtils.unexplodeTarball(dir, copy);
        return artifactReference(copy);
    }

    private ArtifactReference artifactReference(final Path tarball) {
        return new ArtifactReference() {
            @Override
            public void materializeDirectory(Maybe<FreeScope> scope, Path destination) {
                synchronized(ArtifactScope.this) {
                    checkOpen();
                    QbtUtils.mkdirs(destination.getParent());
                    try {
                        Files.createDirectory(destination);
                    }
                    catch(IOException e) {
                        throw ExceptionUtils.commute(e);
                    }
                    TarballUtils.explodeTarball(destination, tarball);
                }
            }

            @Override
            public void materializeTarball(Path destination) {
                synchronized(ArtifactScope.this) {
                    checkOpen();
                    try {
                        Files.copy(tarball, destination);
                    }
                    catch(IOException e) {
                        throw ExceptionUtils.commute(e);
                    }
                }
            }

            @Override
            public ArtifactReference copyInto(ArtifactScope artifactScope) {
                return artifactScope.copyFile(tarball, false);
            }
        };
    }

    public ArtifactReference empty() {
        try(QbtTempDir emptyDir = new QbtTempDir()) {
            return copyDirectory(emptyDir.path);
        }
    }

    @Override
    public synchronized void close() {
        if(root == null) {
            return;
        }
        try {
            root.close();
        }
        finally {
            root = null;
        }
    }
}
