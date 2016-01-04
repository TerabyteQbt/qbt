package qbt.vcs.git;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import misc1.commons.Either;
import misc1.commons.ds.ImmutableSalvagingMap;
import org.apache.commons.lang3.tuple.Pair;
import qbt.QbtHashUtils;
import qbt.QbtTempDir;
import qbt.QbtUtils;
import qbt.VcsTreeDigest;
import qbt.vcs.TreeAccessor;

public class HotGitTreeAccessor implements TreeAccessor {
    private static final Pattern PATTERN = Pattern.compile("^(\\d{6}) (blob|tree) ([0-9a-f]{40})\t(.*)$");

    private final Path dir;
    private final ImmutableSalvagingMap<String, Either<TreeAccessor, Pair<String, HashCode>>> map;

    private volatile VcsTreeDigest digest = null;

    private HotGitTreeAccessor(Path dir, ImmutableSalvagingMap<String, Either<TreeAccessor, Pair<String, HashCode>>> map) {
        this.dir = dir;
        this.map = map;
    }

    public HotGitTreeAccessor(Path dir, VcsTreeDigest tree) {
        this.dir = dir;

        ImmutableSalvagingMap<String, Either<TreeAccessor, Pair<String, HashCode>>> b = ImmutableSalvagingMap.of();
        for(String line : GitUtils.ph(dir, "git", "ls-tree", tree.getRawDigest().toString()).run().requireSuccess().stdout) {
            Matcher m = PATTERN.matcher(line);
            if(!m.matches()) {
                throw new IllegalStateException();
            }
            String mode = m.group(1);
            String type = m.group(2);
            HashCode object = QbtHashUtils.parse(m.group(3));
            String name = decodeName(m.group(4));
            if(type.equals("blob")) {
                b = b.simplePut(name, Either.<TreeAccessor, Pair<String, HashCode>>right(Pair.of(mode, object)));
                continue;
            }
            if(type.equals("tree") && mode.equals("040000")) {
                b = b.simplePut(name, Either.<TreeAccessor, Pair<String, HashCode>>left(new ColdGitTreeAccessor(dir, new VcsTreeDigest(object))));
                continue;
            }
            throw new IllegalStateException();
        }
        this.map = b;
    }

    private static final Either.Visitor<TreeAccessor, Pair<String, HashCode>, Boolean> IS_EITHER_EMPTY_TREE = new Either.Visitor<TreeAccessor, Pair<String, HashCode>, Boolean>() {
        @Override
        public Boolean left(TreeAccessor left) {
            return left.isEmpty();
        }

        @Override
        public Boolean right(Pair<String, HashCode> right) {
            return false;
        }
    };

    private TreeAccessor with(String name, Either<TreeAccessor, Pair<String, HashCode>> child) {
        if(child != null && child.visit(IS_EITHER_EMPTY_TREE)) {
            child = null;
        }

        ImmutableSalvagingMap<String, Either<TreeAccessor, Pair<String, HashCode>>> newMap = map;
        if(child == null) {
            newMap = newMap.simpleRemove(name);
        }
        else {
            newMap = newMap.simplePut(name, child);
        }
        return new HotGitTreeAccessor(dir, newMap);
    }

    @Override
    public TreeAccessor replace(String path, byte[] contents) {
        int i = path.indexOf('/');
        if(i != -1) {
            String path0 = path.substring(0, i);
            String path1 = path.substring(i + 1);
            Either<TreeAccessor, Pair<String, HashCode>> e = map.get(path0);
            TreeAccessor subtreeAccessor = (e == null) ? null : e.leftOrNull();
            if(subtreeAccessor == null) {
                subtreeAccessor = new HotGitTreeAccessor(dir, ImmutableSalvagingMap.<String, Either<TreeAccessor, Pair<String, HashCode>>>of());
            }
            subtreeAccessor = subtreeAccessor.replace(path1, contents);
            return with(path0, Either.<TreeAccessor, Pair<String, HashCode>>left(subtreeAccessor));
        }
        else {
            HashCode object = GitUtils.writeObject(dir, contents);
            return with(path, Either.<TreeAccessor, Pair<String, HashCode>>right(Pair.of("100644", object)));
        }
    }

    @Override
    public Either<TreeAccessor, byte[]> get(String path) {
        int i = path.indexOf('/');
        if(i != -1) {
            String path0 = path.substring(0, i);
            String path1 = path.substring(i + 1);
            Either<TreeAccessor, Pair<String, HashCode>> e = map.get(path0);
            if(e == null) {
                return null;
            }
            TreeAccessor subtreeAccessor = e.leftOrNull();
            if(subtreeAccessor == null) {
                return null;
            }
            return subtreeAccessor.get(path1);
        }
        else {
            Either<TreeAccessor, Pair<String, HashCode>> e = map.get(path);
            if(e == null) {
                return null;
            }
            return e.transformRight((input) -> GitUtils.readObject(dir, input.getRight()));
        }
    }

    @Override
    public TreeAccessor remove(String path) {
        int i = path.indexOf('/');
        if(i != -1) {
            String path0 = path.substring(0, i);
            String path1 = path.substring(i + 1);
            Either<TreeAccessor, Pair<String, HashCode>> e = map.get(path0);
            if(e == null) {
                return this;
            }
            TreeAccessor subtreeAccessor = e.leftOrNull();
            if(subtreeAccessor == null) {
                return this;
            }
            subtreeAccessor = subtreeAccessor.remove(path1);
            return with(path0, Either.<TreeAccessor, Pair<String, HashCode>>left(subtreeAccessor));
        }
        else {
            Either<TreeAccessor, Pair<String, HashCode>> e = map.get(path);
            if(e == null) {
                return this;
            }
            return with(path, null);
        }
    }

    @Override
    public VcsTreeDigest getDigest() {
        VcsTreeDigest digestLocal = digest;
        if(digestLocal == null) {
            ImmutableList.Builder<String> lines = ImmutableList.builder();
            List<Map.Entry<String, Either<TreeAccessor, Pair<String, HashCode>>>> entries = Lists.newArrayList(map.entries());
            Collections.sort(entries, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
            for(final Map.Entry<String, Either<TreeAccessor, Pair<String, HashCode>>> e : entries) {
                lines.add(e.getValue().visit(new Either.Visitor<TreeAccessor, Pair<String, HashCode>, String>() {
                    @Override
                    public String left(TreeAccessor subtreeAccessor) {
                        return "040000 tree " + subtreeAccessor.getDigest().getRawDigest() + "\t" + e.getKey();
                    }

                    @Override
                    public String right(Pair<String, HashCode> p) {
                        return p.getLeft() + " blob " + p.getRight() + "\t" + encodeName(e.getKey());
                    }
                }));
            }

            HashCode rawDigest;
            try(QbtTempDir tempDir = new QbtTempDir()) {
                Path tempFile = tempDir.resolve("object");
                QbtUtils.writeLines(tempFile, lines.build());
                rawDigest = GitUtils.sha1(GitUtils.ph(dir, "git", "mktree").fileInput(tempFile));
            }

            digestLocal = digest = new VcsTreeDigest(rawDigest);
        }
        return digestLocal;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Collection<String> getEntryNames() {
        return map.keys();
    }

    private static String decodeName(String s) {
        if(!s.startsWith("\"")) {
            return s;
        }
        if(s.length() < 2 || !s.endsWith("\"")) {
            throw new IllegalArgumentException("Bad ls-tree name: " + s);
        }
        String sq = s.substring(1, s.length() - 1);
        int i = 0;
        StringBuilder sb = new StringBuilder();
        while(true) {
            if(i == sq.length()) {
                return sb.toString();
            }
            char c = sq.charAt(i);
            if(c != '\\') {
                sb.append(c);
                ++i;
                continue;
            }
            if(i + 1 == sq.length()) {
                throw new IllegalArgumentException("Bad ls-tree name: " + s);
            }
            char c2 = sq.charAt(i + 1);
            switch(c2) {
                case '\\':
                case '"':
                    sb.append(c2);
                    break;

                case 't':
                    sb.append('\t');
                    break;

                case 'r':
                    sb.append('\r');
                    break;

                case 'n':
                    sb.append('\n');
                    break;

                default:
                    throw new IllegalArgumentException("Bad ls-tree name: " + s);
            }
            i += 2;
        }
    }

    private static String encodeName(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for(int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch(c) {
                case '\\':
                case '"':
                    sb.append('\\');
                    sb.append(c);
                    break;

                case '\t':
                    sb.append("\\t");
                    break;

                case '\r':
                    sb.append("\\r");
                    break;

                case '\n':
                    sb.append("\\n");
                    break;

                default:
                    sb.append(c);
                    break;
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
