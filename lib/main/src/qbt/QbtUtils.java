package qbt;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import misc1.commons.ExceptionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Triple;

public final class QbtUtils {
    private QbtUtils() {
        // no
    }

    private static Path findUp(String name, String arg) {
        if(arg != null) {
            return Paths.get(arg).toAbsolutePath();
        }
        Path where = Paths.get(".").toAbsolutePath();
        while(true) {
            Path proposed = where.resolve(name);
            if(Files.isRegularFile(proposed)) {
                return proposed;
            }
            where = where.getParent();
            if(where == null) {
                throw new IllegalArgumentException("Could not find " + name + " and none specified.");
            }
        }
    }

    private static Path findMeta() {
        Path objPath = findUp(".qbt-meta-location", null);
        return objPath.getParent().resolve(readLines(objPath).iterator().next());
    }

    public static Path findInMeta(String name, String overriddenValue) {
        if(overriddenValue != null) {
            return Paths.get(overriddenValue).toAbsolutePath();
        }
        else {
            return findMeta().resolve(name);
        }
    }

    public static void deleteRecursively(Path path, boolean quiet) {
        // Unfortunately java API sucks so bad.
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if(exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                    throw exc;
                }
            });
        }
        catch(IOException e) {
            if(!quiet) {
                throw ExceptionUtils.commute(e);
            }
        }
    }

    public static List<String> readLines(Path p) {
        try {
            return com.google.common.io.Files.readLines(p.toFile(), Charsets.UTF_8);
        }
        catch(IOException e) {
            throw ExceptionUtils.commute(e);
        }
    }

    public static void writeLines(Path p, Iterable<? extends CharSequence> lines) {
        try {
            com.google.common.io.Files.asCharSink(p.toFile(), Charsets.UTF_8).writeLines(lines, "\n");
        }
        catch(IOException e) {
            throw ExceptionUtils.commute(e);
        }
    }

    public static List<Path> listChildren(Path p) {
        try {
            ImmutableList.Builder<Path> b = ImmutableList.builder();
            try(DirectoryStream<Path> ds = Files.newDirectoryStream(p)) {
                for(Path child : ds) {
                    b.add(child);
                }
            }
            return b.build();
        }
        catch(IOException e) {
            throw ExceptionUtils.commute(e);
        }
    }

    public static void delete(Path p) {
        try {
            Files.delete(p);
        }
        catch(IOException e) {
            throw ExceptionUtils.commute(e);
        }
    }

    public static InputStream openRead(Path p) throws FileNotFoundException {
        // silly Files API does't provide guaranteed distinguishing of file not
        // found from other IO error so we resort to the accursed File
        return new FileInputStream(p.toFile());
    }

    public static OutputStream openWrite(Path p) throws FileNotFoundException {
        return new FileOutputStream(p.toFile());
    }

    public static void mkdirs(Path f) {
        try {
            Files.createDirectories(f);
        }
        catch(IOException e) {
            throw ExceptionUtils.commute(e);
        }
    }

    private static final String CONFLICT_MARKERS = "<|=>";

    public static Triple<Iterable<String>, Iterable<String>, Iterable<String>> parseConflictLines(Iterable<String> lines) {
        int state = 0;
        ImmutableList.Builder<String> lhs = ImmutableList.builder();
        ImmutableList.Builder<String> mhs = ImmutableList.builder();
        ImmutableList.Builder<String> rhs = ImmutableList.builder();
        int lineNo = 1;
        for(String line : lines) {
            char marker = parseConflictMarker(line);
            if(marker != 0) {
                if(CONFLICT_MARKERS.charAt(state) != marker) {
                    throw new IllegalArgumentException("Unexpected '" + marker + "' marker on line " + lineNo);
                }
                state = (state + 1) % 4;
            }
            else {
                switch(state) {
                    case 0:
                        lhs.add(line);
                        mhs.add(line);
                        rhs.add(line);
                        break;

                    case 1:
                        lhs.add(line);
                        break;

                    case 2:
                        mhs.add(line);
                        break;

                    case 3:
                        rhs.add(line);
                        break;
                }
            }

            ++lineNo;
        }
        if(state != 0) {
            throw new IllegalArgumentException("Unexpected EOF");
        }
        return Triple.<Iterable<String>, Iterable<String>, Iterable<String>>of(lhs.build(), mhs.build(), rhs.build());
    }

    private static char parseConflictMarker(String s) {
        if(s.isEmpty()) {
            return 0;
        }
        char marker = s.charAt(0);
        if(CONFLICT_MARKERS.indexOf(marker) == -1) {
            return 0;
        }
        for(int i = 1; i < 7; ++i) {
            if(s.charAt(i) != marker) {
                return 0;
            }
        }
        if(s.length() > 7 && s.charAt(7) != ' ') {
            return 0;
        }
        return marker;
    }

    public static boolean semiAtomicDirCache(Path dir, String tempPrefix, Function<Path, ObjectUtils.Null> build) {
        if(Files.isDirectory(dir)) {
            return false;
        }

        Path sibling = dir.resolveSibling(tempPrefix + "." + QbtHashUtils.random());
        final Path buildTarget = sibling.resolve("dir");
        boolean returning = false;
        try {
            Files.createDirectories(buildTarget);
            build.apply(buildTarget);

            try {
                Files.move(buildTarget, dir, StandardCopyOption.ATOMIC_MOVE);
                returning = true;
                return true;
            }
            catch(IOException e) {
                // Move failed, or something?  The java API here sucks
                // so it's very hard to tell what went wrong.  If the
                // cache now exists for any reason we'll swallow.
                if(Files.isDirectory(dir)) {
                    // swallow
                    return false;
                }
                else {
                    throw ExceptionUtils.commute(e);
                }
            }
        }
        catch(IOException e) {
            throw ExceptionUtils.commute(e);
        }
        finally {
            // Now, no matter what happened we'll clean up that temp directory.
            QbtUtils.deleteRecursively(sibling, !returning);
        }
    }
}
