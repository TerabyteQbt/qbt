package qbt.artifactcacher;

import com.google.common.io.ByteStreams;
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

public final class ArtifactReferences {
    private ArtifactReferences() {
        // no
    }

    private static class QbtTempDirHolder implements AutoCloseable {
        private QbtTempDir delegate;

        public QbtTempDirHolder(QbtTempDir delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() {
            if(delegate != null) {
                delegate.close();
            }
        }
    }

    public static ArtifactReference copyFile(FreeScope scope, Path file, boolean allowMiss) {
        InputStream is;
        try {
            is = QbtUtils.openRead(file);
        }
        catch(FileNotFoundException e) {
            if(allowMiss) {
                // nope
                return null;
            }
            throw ExceptionUtils.commute(e);
        }

        final QbtTempDir temp = new QbtTempDir();
        try(QbtTempDirHolder h = new QbtTempDirHolder(temp)) {
            Path copy = temp.resolve(QbtHashUtils.random() + ".tar.gz");
            try(OutputStream os = QbtUtils.openWrite(copy)) {
                ByteStreams.copy(is, os);
            }
            catch(IOException e) {
                throw ExceptionUtils.commute(e);
            }

            // keep
            h.delegate = null;
            return scope.initial(ArtifactReference.TYPE, rawArtifactReference(temp, copy));
        }
    }

    public static ArtifactReference copyDirectory(FreeScope scope, Path dir) {
        final QbtTempDir temp = new QbtTempDir();
        try(QbtTempDirHolder h = new QbtTempDirHolder(temp)) {
            Path copy = temp.resolve(QbtHashUtils.random() + ".tar.gz");
            TarballUtils.unexplodeTarball(dir, copy);

            // keep
            h.delegate = null;
            return scope.initial(ArtifactReference.TYPE, rawArtifactReference(temp, copy));
        }
    }

    public static ArtifactReference empty(FreeScope scope) {
        try(QbtTempDir emptyDir = new QbtTempDir()) {
            return copyDirectory(scope, emptyDir.path);
        }
    }

    private static RawArtifactReference rawArtifactReference(final QbtTempDir temp, final Path tarball) {
        return new RawArtifactReference() {
            @Override
            public void materializeDirectory(Maybe<FreeScope> scope, Path destination) {
                QbtUtils.mkdirs(destination.getParent());
                try {
                    Files.createDirectory(destination);
                }
                catch(IOException e) {
                    throw ExceptionUtils.commute(e);
                }
                TarballUtils.explodeTarball(destination, tarball);
            }

            @Override
            public void materializeTarball(Path destination) {
                try {
                    Files.copy(tarball, destination);
                }
                catch(IOException e) {
                    throw ExceptionUtils.commute(e);
                }
            }

            @Override
            public void free() {
                temp.close();
            }
        };
    }
}
