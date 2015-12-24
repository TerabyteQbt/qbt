package qbt.utils;

import java.nio.file.Path;
import misc1.commons.ph.ProcessHelper;

public final class TarballUtils {
    private TarballUtils() {
        // no
    }

    public static void unexplodeTarball(Path dir, Path tarball) {
        ProcessHelper.of(dir, "tar", "-zpcf", tarball.toAbsolutePath().toString(), ".").run().requireSuccess();
    }

    public static void explodeTarball(Path dir, Path tarball) {
        ProcessHelper.of(dir, "tar", "-zxf", tarball.toAbsolutePath().toString()).run().requireSuccess();
    }
}
