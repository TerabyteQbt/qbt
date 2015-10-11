package qbt;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import misc1.commons.ExceptionUtils;

public final class QbtHashUtils {
    private QbtHashUtils() {
    }

    public static HashFunction hashFunction() {
        return Hashing.sha1();
    }

    public static Hasher newHasher() {
        return hashFunction().newHasher();
    }

    private static int parseNibble(char c) {
        if(c >= '0' && c <= '9') {
            return c - '0';
        }
        if(c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if(c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        throw new IllegalArgumentException("Invalid nibble: " + c);
    }

    public static HashCode parse(String s) {
        if(s.length() != 40) {
            throw new IllegalArgumentException("Invalid digest: " + s);
        }
        byte[] bytes = new byte[20];
        for(int i = 0; i < 20; ++i) {
            bytes[i] |= parseNibble(s.charAt(2 * i)) << 4;
            bytes[i] |= parseNibble(s.charAt(2 * i + 1));
        }
        return HashCode.fromBytes(bytes);
    }

    public static HashCode random() {
        byte[] b = new byte[20];
        new Random().nextBytes(b);
        return HashCode.fromBytes(b);
    }

    public static HashCode of(Object... pieces) {
        Hasher b = newHasher();
        for(int i = 0; i < pieces.length; ++i) {
            if(i > 0) {
                b = b.putString("\t", Charsets.UTF_8);
            }
            String s = String.valueOf(pieces[i]);
            b = b.putString(s.replace("\\",  "\\\\").replace("\t", "\\t"), Charsets.UTF_8);
        }
        return b.hash();
    }

    public static HashCode hash(Path p) {
        return hash(p, hashFunction());
    }

    public static HashCode hash(Path p, HashFunction hashFunction) {
        try {
            return com.google.common.io.Files.hash(p.toFile(), hashFunction);
        }
        catch(IOException e) {
            throw ExceptionUtils.commute(e);
        }
    }
}
