package qbt.vcs.git;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import misc1.commons.Either;
import org.apache.commons.lang3.tuple.Pair;
import qbt.QbtHashUtils;
import qbt.QbtTempDir;
import qbt.QbtUtils;
import qbt.VcsTreeDigest;
import qbt.utils.ProcessHelper;
import qbt.vcs.TreeAccessor;

public class HotGitTreeAccessor implements TreeAccessor {
    private static final Pattern PATTERN = Pattern.compile("^(\\d{6}) (blob|tree) ([0-9a-f]{40})\t(.*)$");

    private final Path dir;
    private final Map<String, Either<TreeAccessor, Pair<String, HashCode>>> map;

    private volatile VcsTreeDigest digest = null;

    private HotGitTreeAccessor(Path dir, Map<String, Either<TreeAccessor, Pair<String, HashCode>>> map) {
        this.dir = dir;
        this.map = map;
    }

    public HotGitTreeAccessor(Path dir, VcsTreeDigest tree) {
        this.dir = dir;

        ImmutableMap.Builder<String, Either<TreeAccessor, Pair<String, HashCode>>> b = ImmutableMap.builder();

        for(String line : new ProcessHelper(dir, "git", "ls-tree", tree.getRawDigest().toString()).inheritError().completeLines()) {
            Matcher m = PATTERN.matcher(line);
            if(!m.matches()) {
                throw new IllegalStateException();
            }
            String mode = m.group(1);
            String type = m.group(2);
            HashCode object = QbtHashUtils.parse(m.group(3));
            String name = m.group(4);
            if(type.equals("blob")) {
                b.put(name, Either.<TreeAccessor, Pair<String, HashCode>>right(Pair.of(mode, object)));
                continue;
            }
            if(type.equals("tree") && mode.equals("040000")) {
                b.put(name, Either.<TreeAccessor, Pair<String, HashCode>>left(new ColdGitTreeAccessor(dir, new VcsTreeDigest(object))));
                continue;
            }
            throw new IllegalStateException();
        }

        this.map = b.build();
    }

    private TreeAccessor with(String name, Either<TreeAccessor, Pair<String, HashCode>> child) {
        Map<String, Either<TreeAccessor, Pair<String, HashCode>>> newMap = Maps.newTreeMap();
        newMap.putAll(map);
        if(child == null) {
            newMap.remove(name);
        }
        else {
            newMap.put(name, child);
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
                subtreeAccessor = new HotGitTreeAccessor(dir, ImmutableMap.<String, Either<TreeAccessor, Pair<String, HashCode>>>of());
            }
            subtreeAccessor = subtreeAccessor.replace(path1, contents);
            if(subtreeAccessor.isEmpty()) {
                return with(path0, null);
            }
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
            Pair<String, HashCode> p = e.rightOrNull();
            if(p == null) {
                return null;
            }
            return Either.<TreeAccessor, byte[]>right(GitUtils.readObject(dir, p.getRight()));
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
                return null;
            }
            return with(path, null);
        }
    }

    @Override
    public VcsTreeDigest getDigest() {
        VcsTreeDigest digestLocal = digest;
        if(digestLocal == null) {
            ImmutableList.Builder<String> lines = ImmutableList.builder();
            for(final Map.Entry<String, Either<TreeAccessor, Pair<String, HashCode>>> e : map.entrySet()) {
                lines.add(e.getValue().visit(new Either.Visitor<TreeAccessor, Pair<String, HashCode>, String>() {
                    @Override
                    public String left(TreeAccessor subtreeAccessor) {
                        return "040000 tree " + subtreeAccessor.getDigest().getRawDigest() + "\t" + e.getKey();
                    }

                    @Override
                    public String right(Pair<String, HashCode> p) {
                        return p.getLeft() + " blob " + p.getRight() + "\t" + e.getKey();
                    }
                }));
            }

            HashCode rawDigest;
            try(QbtTempDir tempDir = new QbtTempDir()) {
                Path tempFile = tempDir.resolve("object");
                QbtUtils.writeLines(tempFile, lines.build());
                rawDigest = new ProcessHelper(dir, "git", "mktree").fileInput(tempFile).inheritError().completeSha1();
            }

            digestLocal = digest = new VcsTreeDigest(rawDigest);
        }
        return digestLocal;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
}
