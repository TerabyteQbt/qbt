package qbt.vcs.git;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import misc1.commons.ExceptionUtils;
import misc1.commons.ph.ProcessHelper;
import qbt.QbtHashUtils;
import qbt.QbtTempDir;
import qbt.QbtUtils;
import qbt.TypedDigest;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.utils.ProcessHelperUtils;
import qbt.vcs.CommitData;
import qbt.vcs.CommitLevel;

public class GitUtils {
    private GitUtils() {
        // no
    }

    public static final VcsTreeDigest EMPTY_TREE = new VcsTreeDigest(QbtHashUtils.parse("4b825dc642cb6eb9a060e54bf8d69288fbee4904"));

    static ProcessHelper ph(Path dir, String... cmd) {
        return ProcessHelper.of(dir, cmd).apply(ProcessHelperUtils::stripGitEnv);
    }

    static HashCode sha1(ProcessHelper ph) {
        return QbtHashUtils.parse(ph.run().requireLine().substring(0, 40));
    }

    private static void runQuiet(Path dir, String... cmd) {
        ph(dir, cmd).run().requireSuccess();
    }

    private static String getPrefix(Path dir) {
        return ph(dir, "git", "rev-parse", "--show-prefix").run().requireLine();
    }

    public static Path getRoot(Path dir) {
        return Paths.get(ph(dir, "git", "rev-parse", "--show-toplevel").run().requireLine());
    }

    private static VcsTreeDigest getSubIndex(Path dir) {
        return getSubIndex(dir, null);
    }

    private static VcsTreeDigest getSubIndex(Path dir, Path tempIndexFile) {
        ProcessHelper p;

        String prefix = getPrefix(dir);
        p = ph(getRoot(dir), "git", "ls-files", "--", prefix);
        if(tempIndexFile != null) {
            p = p.putEnv("GIT_INDEX_FILE", tempIndexFile.toAbsolutePath().toString());
        }
        if(p.run().requireSuccess().stdout.isEmpty()) {
            return EMPTY_TREE;
        }

        p = ph(dir, "git", "write-tree", "--prefix=" + prefix);
        if(tempIndexFile != null) {
            p = p.putEnv("GIT_INDEX_FILE", tempIndexFile.toAbsolutePath().toString());
        }
        return new VcsTreeDigest(sha1(p));
    }

    public static boolean isClean(Path dir) {
        return ph(dir, "git", "status", "--porcelain", ".").run().requireSuccess().stdout.isEmpty();
    }

    public static boolean isClean(Path dir, CommitLevel level) {
        VcsTreeDigest headTree = getSubtree(dir, getCurrentCommit(dir), "");
        VcsTreeDigest workTree = getWorkingTree(dir, level);
        return headTree.equals(workTree);
    }

    public static boolean objectExists(Path repo, TypedDigest object) {
        return ph(repo, "git", "cat-file", "-e", String.valueOf(object.getRawDigest())).run().exitCode == 0;
    }

    public static boolean isAncestorOf(Path repo, VcsVersionDigest ancestor, VcsVersionDigest descendent) {
        return ph(repo, "git", "merge-base", "--is-ancestor", String.valueOf(ancestor.getRawDigest()), String.valueOf(descendent.getRawDigest())).run().exitCode == 0;
    }

    public static Iterable<String> showFile(Path repo, VcsVersionDigest commit, String path) {
        return ph(repo, "git", "show", commit.getRawDigest() + ":" + path).run().requireSuccess().stdout;
    }

    public static Iterable<String> showFile(Path repo, VcsTreeDigest tree, String path) {
        return ph(repo, "git", "show", tree.getRawDigest() + ":" + path).run().requireSuccess().stdout;
    }

    public static VcsTreeDigest getSubtree(Path repo, VcsVersionDigest commit, String path) {
        return new VcsTreeDigest(sha1(ph(repo, "git", "rev-parse", commit.getRawDigest() + ":" + path)));
    }

    public static VcsTreeDigest getSubtree(Path repo, VcsTreeDigest tree, String path) {
        return new VcsTreeDigest(sha1(ph(repo, "git", "rev-parse", tree.getRawDigest() + ":" + path)));
    }

    public static boolean remoteExists(String spec) {
        return ph(Paths.get("/"), "git", "ls-remote", spec).run().exitCode == 0;
    }

    public static void createWorkingRepo(Path repo) {
        runQuiet(repo, "git", "init", "-q");
    }

    public static void createCacheRepo(Path repo) {
        runQuiet(repo, "git", "init", "-q", "--bare");
    }

    private static void archiveTree(Path repo, VcsTreeDigest tree, Path tar) {
        runQuiet(repo, "git", "archive", "-o", tar.toAbsolutePath().toString(), String.valueOf(tree.getRawDigest()));
    }

    private static void explodeTar(Path dir, Path tar) {
        runQuiet(dir, "tar", "-xf", tar.toAbsolutePath().toString());
    }

    public static void checkoutTree(Path repo, VcsTreeDigest tree, Path path) {
        try(QbtTempDir tempDir = new QbtTempDir()) {
            Path tarFile = tempDir.resolve(tree.getRawDigest() + ".tar");
            archiveTree(repo, tree, tarFile);
            explodeTar(path, tarFile);
        }
    }

    public static VcsVersionDigest getCurrentCommit(Path dir) {
        return new VcsVersionDigest(sha1(ph(dir, "git", "rev-parse", "HEAD")));
    }

    public static void checkout(Path dir, VcsVersionDigest commit) {
        runQuiet(dir, "git", "checkout", "-q", String.valueOf(commit.getRawDigest()));
    }

    public static void merge(Path dir, VcsVersionDigest commit) {
        runQuiet(dir, "git", "merge", "-q", String.valueOf(commit.getRawDigest()));
    }

    public static void rebase(Path dir, VcsVersionDigest from, VcsVersionDigest to) {
        runQuiet(dir, "git", "rebase", "--onto", "HEAD", from.getRawDigest().toString(), to.getRawDigest().toString());
    }

    public static void fetchPins(Path dir, String remote) {
        runQuiet(dir, "git", "fetch", "-q", remote, "refs/qbt-pins/*:refs/qbt-pins/*");
    }

    public static void addPinToRemote(Path dir, String remote, VcsVersionDigest commit) {
        runQuiet(dir, "git", "push", "-q", remote, commit.getRawDigest() + ":refs/qbt-pins/" + commit.getRawDigest());
    }

    private static String[] commitLevelAdd(CommitLevel level) {
        // Oh you daffy, daffy git fuckers and your confusing at best options
        // for `git add` and `git commit`.  Obviously our bottom level should
        // add nothing (just use index) and our top level should add
        // everything, but what to do about medium level(s)?  `git commit -a`
        // and `git add -u` do the same thing which is what we choose for our
        // one medium level, I guess...

        switch(level) {
            case UNTRACKED:
                return new String[] { "git", "add", "-A", "." };

            case MODIFIED:
                return new String[] { "git", "add", "-u", "." };

            case STAGED:
                return null;
        }
        throw new IllegalStateException();
    }

    public static VcsTreeDigest getWorkingTree(Path dir, CommitLevel level) {
        Path realIndexFile = dir.resolve(ph(dir, "git", "rev-parse", "--git-dir").run().requireLine()).resolve("index").toAbsolutePath();

        try(QbtTempDir tempDir = new QbtTempDir()) {
            Path tempIndexFile = tempDir.resolve("temp.index");
            try(InputStream is = QbtUtils.openRead(realIndexFile); OutputStream os = QbtUtils.openWrite(tempIndexFile)) {
                ByteStreams.copy(is, os);
            }
            String[] commitLevelAdd = commitLevelAdd(level);
            if(commitLevelAdd != null) {
                ph(dir, commitLevelAdd).putEnv("GIT_INDEX_FILE", tempIndexFile.toString()).run().requireSuccess();
            }
            return getSubIndex(dir, tempIndexFile);
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static VcsVersionDigest revParse(Path dir, String arg) {
        return new VcsVersionDigest(sha1(ph(dir, "git", "rev-parse", arg)));
    }

    public static Multimap<String, String> getAllConfig(Path dir) {
        ImmutableMultimap.Builder<String, String> b = ImmutableMultimap.builder();
        for(String line : ph(dir, "git", "config", "-l").run().requireSuccess().stdout) {
            int i = line.indexOf('=');
            if(i == -1) {
                throw new IllegalStateException("Bogus git config -l line: " + line);
            }
            b.put(line.substring(0, i), line.substring(i + 1));
        }
        return b.build();
    }

    public static VcsVersionDigest commit(Path dir, boolean amend, String message, CommitLevel level) {
        String[] commitLevelAdd = commitLevelAdd(level);
        if(commitLevelAdd != null) {
            runQuiet(dir, commitLevelAdd);
        }
        if(amend) {
            runQuiet(dir, "git", "commit", "--amend", "-m" + message);
        }
        else {
            runQuiet(dir, "git", "commit", "-m" + message);
        }
        return getCurrentCommit(dir);
    }

    public static Map<VcsVersionDigest, CommitData> revWalk(Path dir, Collection<VcsVersionDigest> from, Collection<VcsVersionDigest> to) {
        if(to.isEmpty()) {
            return ImmutableMap.of();
        }
        ImmutableList.Builder<String> args = ImmutableList.builder();
        args.add("git", "log", "--format=raw");
        for(VcsVersionDigest e : from) {
            args.add("^" + e.getRawDigest());
        }
        for(VcsVersionDigest e : to) {
            args.add(String.valueOf(e.getRawDigest()));
        }
        return parseRawLog(dir, args.build().toArray(new String[0]));
    }

    public static CommitData getCommitData(Path dir, VcsVersionDigest commit) {
        return parseRawLog(dir, "git", "log", "--format=raw", "-1", commit.getRawDigest().toString()).get(commit);
    }

    private static final Pattern COMMITTER_PATTERN = Pattern.compile("^committer ([^<>]*) <([^<>]*)> ([0-9]*).*$");
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("^author ([^<>]*) <([^<>]*)> ([0-9]*).*$");

    private static Map<VcsVersionDigest, CommitData> parseRawLog(Path dir, String... args) {
        class Parser {
            private final ImmutableMap.Builder<VcsVersionDigest, CommitData> b = ImmutableMap.builder();
            private VcsVersionDigest currentCommit = null;
            private VcsTreeDigest currentTree = null;
            private ImmutableList.Builder<VcsVersionDigest> currentParents = ImmutableList.builder();
            private String authorName = null;
            private String authorEmail = null;
            private String authorDate = null;
            private String committerName = null;
            private String committerEmail = null;
            private String committerDate = null;
            private StringBuilder currentMessage = new StringBuilder();

            private void flush() {
                if(currentCommit != null) {
                    if(currentTree == null) {
                        throw new IllegalStateException();
                    }
                    if(authorName == null) {
                        throw new IllegalStateException();
                    }
                    if(authorEmail == null) {
                        throw new IllegalStateException();
                    }
                    if(authorDate == null) {
                        throw new IllegalStateException();
                    }
                    if(committerName == null) {
                        throw new IllegalStateException();
                    }
                    if(committerEmail == null) {
                        throw new IllegalStateException();
                    }
                    if(committerDate == null) {
                        throw new IllegalStateException();
                    }
                    CommitData.Builder cd = CommitData.TYPE.builder();
                    cd = cd.set(CommitData.TREE, currentTree);
                    cd = cd.set(CommitData.PARENTS, currentParents.build());
                    cd = cd.set(CommitData.AUTHOR_NAME, authorName);
                    cd = cd.set(CommitData.AUTHOR_EMAIL, authorEmail);
                    cd = cd.set(CommitData.AUTHOR_DATE, authorDate);
                    cd = cd.set(CommitData.COMMITTER_NAME, committerName);
                    cd = cd.set(CommitData.COMMITTER_EMAIL, committerEmail);
                    cd = cd.set(CommitData.COMMITTER_DATE, committerDate);
                    cd = cd.set(CommitData.MESSAGE, currentMessage.toString());
                    b.put(currentCommit, cd.build());
                    currentCommit = null;
                    currentTree = null;
                    currentParents = ImmutableList.builder();
                    authorName = null;
                    authorEmail = null;
                    committerName = null;
                    committerEmail = null;
                    currentMessage = new StringBuilder();
                }
            }

            public void parseLine(String line) {
                if(line.startsWith("commit ")) {
                    flush();
                    currentCommit = new VcsVersionDigest(QbtHashUtils.parse(line.substring(7)));
                }
                if(line.startsWith("tree ")) {
                    currentTree = new VcsTreeDigest(QbtHashUtils.parse(line.substring(5)));
                }
                if(line.startsWith("parent ")) {
                    currentParents.add(new VcsVersionDigest(QbtHashUtils.parse(line.substring(7))));
                }
                Matcher authorMatcher = AUTHOR_PATTERN.matcher(line);
                if(authorMatcher.matches()) {
                    authorName = authorMatcher.group(1);
                    authorEmail = authorMatcher.group(2);
                    authorDate = authorMatcher.group(3);
                }
                Matcher committerMatcher = COMMITTER_PATTERN.matcher(line);
                if(committerMatcher.matches()) {
                    committerName = committerMatcher.group(1);
                    committerEmail = committerMatcher.group(2);
                    committerDate = committerMatcher.group(3);
                }
                if(line.startsWith("    ")) {
                    if(currentMessage.length() > 0) {
                        currentMessage.append('\n');
                    }
                    currentMessage.append(line.substring(4));
                }
            }

            public Map<VcsVersionDigest, CommitData> eof() {
                flush();
                return b.build();
            }
        }
        Parser pp = new Parser();
        ProcessHelper p = ph(dir, args);
        for(String line : p.run().requireSuccess().stdout) {
            pp.parseLine(line);
        }
        return pp.eof();
    }

    public static VcsVersionDigest createCommit(Path dir, CommitData commitData) {
        ImmutableList.Builder<String> commitTreeCommand = ImmutableList.builder();
        commitTreeCommand.add("git", "commit-tree", "-m", commitData.get(CommitData.MESSAGE));
        for(VcsVersionDigest parent : commitData.get(CommitData.PARENTS)){
            commitTreeCommand.add("-p", parent.getRawDigest().toString());
        }
        commitTreeCommand.add(commitData.get(CommitData.TREE).getRawDigest().toString());
        ProcessHelper p = ph(dir, commitTreeCommand.build().toArray(new String[0]));
        p = p.putEnv("GIT_AUTHOR_NAME", commitData.get(CommitData.AUTHOR_NAME));
        p = p.putEnv("GIT_AUTHOR_EMAIL", commitData.get(CommitData.AUTHOR_EMAIL));
        p = p.putEnv("GIT_AUTHOR_DATE", commitData.get(CommitData.AUTHOR_DATE));
        p = p.putEnv("GIT_COMMITTER_NAME", commitData.get(CommitData.COMMITTER_NAME));
        p = p.putEnv("GIT_COMMITTER_EMAIL", commitData.get(CommitData.COMMITTER_EMAIL));
        p = p.putEnv("GIT_COMMITTER_DATE", commitData.get(CommitData.COMMITTER_DATE));
        HashCode object = sha1(p);
        return new VcsVersionDigest(object);
    }

    public static HashCode writeObject(Path dir, byte[] contents) {
        try(QbtTempDir tempDir = new QbtTempDir()) {
            Path tempFile = tempDir.resolve("object");
            try {
                Files.write(contents, tempFile.toFile());
            }
            catch(IOException e) {
                throw ExceptionUtils.commute(e);
            }
            return sha1(ph(dir, "git", "hash-object", "-w", tempFile.toString()));
        }
    }

    public static byte[] readObject(Path dir, HashCode object) {
        try(QbtTempDir tempDir = new QbtTempDir()) {
            Path tempFile = tempDir.resolve("object");
            ph(dir, "git", "show", object.toString()).fileOutput(tempFile).run().requireSuccess();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                Files.copy(tempFile.toFile(), baos);
            }
            catch(IOException e) {
                throw ExceptionUtils.commute(e);
            }
            return baos.toByteArray();
        }
    }

    public static List<String> getUserVisibleStatus(Path dir) {
        return ph(dir, "git", "status", "--short").run().requireSuccess().stdout;
    }
}
