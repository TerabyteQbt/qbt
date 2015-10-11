package qbt.utils;

import java.nio.file.Path;

public final class TarballUtils {
    private TarballUtils() {
        // no
    }

    public static void unexplodeTarball(Path dir, Path tarball) {
        ProcessHelperUtils.runQuiet(dir, "tar", "-zpcf", tarball.toAbsolutePath().toString(), ".");
    }

    public static void explodeTarball(Path dir, Path tarball) {
        ProcessHelperUtils.runQuiet(dir, "tar", "-zxf", tarball.toAbsolutePath().toString());
    }
}
