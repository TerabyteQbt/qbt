package qbt.artifactcacher;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import misc1.commons.ExceptionUtils;
import misc1.commons.resources.FreeScope;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.QbtUtils;
import qbt.recursive.cv.CumulativeVersionDigest;

public class LocalArtifactCacher implements ArtifactCacher {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalArtifactCacher.class);

    private static final Pattern TARBALL_PATTERN = Pattern.compile("^[0-9a-f]{40}.tar.gz$");

    private final Path root;
    private final long targetSize;

    public LocalArtifactCacher(Path root, long targetSize) {
        this.root = root;
        this.targetSize = targetSize;
    }

    private Path formatAndTouch(CumulativeVersionDigest key) {
        Path tarball = root.resolve(key.getRawDigest().toString() + ".tar.gz");
        try {
            Files.setLastModifiedTime(tarball, FileTime.fromMillis(System.currentTimeMillis()));
        }
        catch(IOException e) {
            // yearggh, this is really best effort and we can't do this (check
            // and set) non-racily with the Files API
        }
        return tarball;
    }

    @Override
    public Pair<Architecture, ArtifactReference> get(FreeScope scope, CumulativeVersionDigest key, Architecture arch) {
        Path tarball = formatAndTouch(key);
        ArtifactReference ret = ArtifactReferences.copyFile(scope, tarball, true);
        LOGGER.debug("Cache check for " + key + " at " + tarball + " " + (ret == null ? "missed" : "hit"));
        return ret == null ? null : Pair.of(Architecture.unknown(), ret);
    }

    @Override
    public void touch(CumulativeVersionDigest key, Architecture arch) {
        formatAndTouch(key);
    }

    @Override
    public Pair<Architecture, ArtifactReference> intercept(FreeScope scope, CumulativeVersionDigest key, Pair<Architecture, ArtifactReference> p) {
        simplePut(p.getLeft(), key, p.getRight());
        return p;
    }

    private void simplePut(Architecture arch, CumulativeVersionDigest key, ArtifactReference artifact) {
        Path cacheFile = root.resolve(key.getRawDigest().toString() + ".tar.gz");
        if(Files.isRegularFile(cacheFile)) {
            return;
        }
        Path tmpFile = root.resolve("." + key.getRawDigest().toString() + ".tar.gz." + UUID.randomUUID().toString());
        try {
            QbtUtils.mkdirs(root);
            artifact.materializeTarball(tmpFile);
            // This sucks madly, I can't generally distinguish some jackass
            // throw from the file being there and failing the move.  Thanks
            // shitty java API.
            try {
                Files.move(tmpFile, cacheFile, StandardCopyOption.ATOMIC_MOVE);
            }
            catch(FileAlreadyExistsException e) {
                // swallow if the java API is kind enough to tell us that this
                // is why it failed
            }
            catch(IOException e) {
                if(Files.isRegularFile(cacheFile)) {
                    return;
                }

                // Here is the real problem -- if the java API fails to give us
                // FileAlreadyExistsException on a collision and then someone
                // cleans up the file we get here and throw when we really
                // shouldn't.  We'll take that over swallowing all exceptions
                // here.
                throw e;
            }
        }
        catch(IOException e) {
            throw ExceptionUtils.commute(e);
        }
        finally {
            // no matter what happened
            if(Files.isRegularFile(tmpFile)) {
                QbtUtils.delete(tmpFile);
            }
        }
    }

    @Override
    public void cleanup() {
        try {
            if(targetSize < 0) {
                return;
            }
            List<Pair<Path, Long>> pairs = Lists.newArrayList();
            if(!Files.isDirectory(root)) {
                return;
            }
            for(Path child : QbtUtils.listChildren(root)) {
                if(!TARBALL_PATTERN.matcher(child.getFileName().toString()).matches()) {
                    continue;
                }
                pairs.add(Pair.of(child, Files.getLastModifiedTime(child).toMillis()));
            }
            Collections.sort(pairs, (a, b) -> {
                if(a.getRight() > b.getRight()) {
                    return -1;
                }
                if(a.getRight() < b.getRight()) {
                    return 1;
                }
                return a.getLeft().compareTo(b.getLeft());
            });
            long totalSize = 0;
            for(Pair<Path, Long> pair : pairs) {
                Path tarball = pair.getLeft();
                if(totalSize >= targetSize) {
                    QbtUtils.delete(tarball);
                }
                else {
                    totalSize += Files.size(tarball);
                }
            }
        }
        catch(IOException e) {
            throw ExceptionUtils.commute(e);
        }
    }
}
