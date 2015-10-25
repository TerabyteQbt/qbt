package qbt.vcs.git;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import misc1.commons.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import qbt.QbtHashUtils;
import qbt.QbtTempDir;
import qbt.QbtUtils;
import qbt.TypedDigest;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.utils.ProcessHelper;
import qbt.utils.ProcessHelperUtils;
import qbt.vcs.CommitData;

public class GitUtils {
    private GitUtils() {
        // no
    }

    public static final VcsTreeDigest EMPTY_TREE = new VcsTreeDigest(QbtHashUtils.parse("4b825dc642cb6eb9a060e54bf8d69288fbee4904"));

    private static String getPrefix(Path dir) {
        ProcessHelper p = new ProcessHelper(dir, "git", "rev-parse", "--show-prefix");
        p = p.inheritError();
        return p.completeLine();
    }

    public static Path getRoot(Path dir) {
        ProcessHelper p = new ProcessHelper(dir, "git", "rev-parse", "--show-toplevel");
        p = p.inheritError();
        return Paths.get(p.completeLine());
    }

    private static VcsTreeDigest getSubIndex(Path dir) {
        return getSubIndex(dir, null);
    }

    private static VcsTreeDigest getSubIndex(Path dir, Path tempIndexFile) {
        ProcessHelper p;

        String prefix = getPrefix(dir);
        p = new ProcessHelper(getRoot(dir), "git", "ls-files", "--", prefix);
        p = p.inheritError();
        if(tempIndexFile != null) {
            p = p.putEnv("GIT_INDEX_FILE", tempIndexFile.toAbsolutePath().toString());
        }
        if(p.completeWasEmpty()) {
            return EMPTY_TREE;
        }

        p = new ProcessHelper(dir, "git", "write-tree", "--prefix=" + prefix);
        p = p.inheritError();
        if(tempIndexFile != null) {
            p = p.putEnv("GIT_INDEX_FILE", tempIndexFile.toAbsolutePath().toString());
        }
        return new VcsTreeDigest(p.completeSha1());
    }

    public static boolean isHeadClean(Path dir) {
        Path root = getRoot(dir);

        // are there dirty, deleted, or untracked files?
        ProcessHelper p = new ProcessHelper(dir, "git", "ls-files", "--deleted", "--modified", "--others", "--exclude-standard").inheritError();
        if(p.completeLines().size() > 0) {
            return false;
        }

        // are there staged (but not committed) changes?
        if(!new ProcessHelper(dir, "git", "diff-index", "--cached", "--quiet", "HEAD").inheritError().completeWasSuccess()) {
            return false;
        }

        // is does the working tree match HEAD?
        VcsTreeDigest headTree = getSubtree(root, getCurrentCommit(root), "");
        if(!getWorkingTreeTree(root).equals(headTree)) {
            return false;
        }

        // does HEAD match the staged index?
        if(!getSubIndex(root).equals(headTree)) {
            return false;
        }

        // looks plausible
        return true;
    }

    public static boolean objectExists(Path repo, TypedDigest object) {
        ProcessHelper p = new ProcessHelper(repo, "git", "cat-file", "-e", String.valueOf(object.getRawDigest()));
        p = p.inheritOutput();
        p = p.inheritError();
        return p.completeWasSuccess();
    }

    public static boolean isAncestorOf(Path repo, VcsVersionDigest ancestor, VcsVersionDigest descendent) {
        ProcessHelper p = new ProcessHelper(repo, "git", "merge-base", "--is-ancestor", String.valueOf(ancestor.getRawDigest()), String.valueOf(descendent.getRawDigest()));
        p = p.inheritOutput();
        p = p.inheritError();
        return p.completeWasSuccess();
    }

    public static void fetchRemote(Path repo, String name) {
        ProcessHelperUtils.runQuiet(repo, "git", "fetch", "-q", name);
    }

    public static Iterable<String> showFile(Path repo, VcsVersionDigest commit, String path) {
        ProcessHelper p = new ProcessHelper(repo, "git", "show", commit.getRawDigest() + ":" + path);
        p = p.inheritError();
        return p.completeLines();
    }

    public static Iterable<String> showFile(Path repo, VcsTreeDigest tree, String path) {
        ProcessHelper p = new ProcessHelper(repo, "git", "show", tree.getRawDigest() + ":" + path);
        p = p.inheritError();
        return p.completeLines();
    }

    public static VcsTreeDigest getSubtree(Path repo, VcsVersionDigest commit, String path) {
        ProcessHelper p = new ProcessHelper(repo, "git", "rev-parse", commit.getRawDigest() + ":" + path);
        p = p.inheritError();
        return new VcsTreeDigest(p.completeSha1());
    }

    public static boolean remoteExists(String spec) {
        ProcessHelper p = new ProcessHelper(Paths.get("/"), "git", "ls-remote", spec);
        p = p.ignoreOutput();
        p = p.ignoreError();
        return p.completeWasSuccess();
    }

    public static void createWorkingRepo(Path repo) {
        ProcessHelperUtils.runQuiet(repo, "git", "init", "-q");
    }

    public static void createCacheRepo(Path repo) {
        ProcessHelperUtils.runQuiet(repo, "git", "init", "-q", "--bare");
    }

    public static void addRemote(Path repo, String name, String spec) {
        ProcessHelperUtils.runQuiet(repo, "git", "remote", "add", name, spec);
    }

    private static void archiveTree(Path repo, VcsTreeDigest tree, Path tar) {
        ProcessHelperUtils.runQuiet(repo, "git", "archive", "-o", tar.toAbsolutePath().toString(), String.valueOf(tree.getRawDigest()));
    }

    private static void explodeTar(Path dir, Path tar) {
        ProcessHelperUtils.runQuiet(dir, "tar", "-xf", tar.toAbsolutePath().toString());
    }

    public static void checkoutTree(Path repo, VcsTreeDigest tree, Path path) {
        try(QbtTempDir tempDir = new QbtTempDir()) {
            Path tarFile = tempDir.resolve(tree.getRawDigest() + ".tar");
            archiveTree(repo, tree, tarFile);
            explodeTar(path, tarFile);
        }
    }

    public static VcsVersionDigest getCurrentCommit(Path dir) {
        ProcessHelper p = new ProcessHelper(dir, "git", "rev-parse", "HEAD");
        p = p.inheritError();
        return new VcsVersionDigest(p.completeSha1());
    }

    public static void checkout(Path dir, VcsVersionDigest commit) {
        ProcessHelperUtils.runQuiet(dir, "git", "checkout", "-q", String.valueOf(commit.getRawDigest()));
    }

    public static void checkout(Path dir, String branchName) {
        ProcessHelperUtils.runQuiet(dir, "git", "checkout", "-q", branchName);
    }

    public static void merge(Path dir, VcsVersionDigest commit) {
        ProcessHelperUtils.runQuiet(dir, "git", "merge", "-q", String.valueOf(commit.getRawDigest()));
    }

    public static void rebase(Path dir, VcsVersionDigest from, VcsVersionDigest to) {
        ProcessHelperUtils.runQuiet(dir, "git", "rebase", "--onto", "HEAD", from.getRawDigest().toString(), to.getRawDigest().toString());
    }

    public static void fetchPins(Path dir, String remote) {
        ProcessHelperUtils.runQuiet(dir, "git", "fetch", "-q", remote, "refs/qbt-pins/*:refs/qbt-pins/*");
    }

    public static void addPinToRemote(Path dir, String remote, VcsVersionDigest commit) {
        ProcessHelperUtils.runQuiet(dir, "git", "push", "-q", remote, commit.getRawDigest() + ":refs/qbt-pins/" + commit.getRawDigest());
    }

    public static void addLocalPinToRemote(Path dir, String remote, VcsVersionDigest commit) {
        ProcessHelperUtils.runQuiet(dir, "git", "push", "-q", remote, commit.getRawDigest() + ":refs/qbt-local-pins/" + commit.getRawDigest());
    }

    private static int manageLocalPins(Path dir) {
        ImmutableList.Builder<VcsVersionDigest> localPinsBuilder = ImmutableList.builder();
        for(String line : new ProcessHelper(dir, "git", "rev-parse", "--glob=refs/qbt-local-pins/*").inheritError().completeLines()) {
            localPinsBuilder.add(new VcsVersionDigest(QbtHashUtils.parse(line)));
        }
        List<VcsVersionDigest> localPins = localPinsBuilder.build();
        ImmutableList.Builder<VcsVersionDigest> cachedPinsBuilder = ImmutableList.builder();
        for(String line : new ProcessHelper(dir, "git", "rev-parse", "--glob=refs/qbt-pins/*").inheritError().completeLines()) {
            cachedPinsBuilder.add(new VcsVersionDigest(QbtHashUtils.parse(line)));
        }
        List<VcsVersionDigest> cachedPins = cachedPinsBuilder.build();

        Set<VcsVersionDigest> pinsKeep = Sets.newHashSet();
        pinsKeep.addAll(localPins);
        pinsKeep.addAll(cachedPins);
        pinsKeep = Sets.newHashSet(mergeBaseIndependent(dir, pinsKeep));
        pinsKeep.removeAll(cachedPins);

        int kept = 0;
        for(VcsVersionDigest localPin : localPins) {
            if(!pinsKeep.contains(localPin)) {
                ProcessHelperUtils.runQuiet(dir, "git", "update-ref", "-d", "refs/qbt-local-pins/" + localPin.getRawDigest());
            }
            else {
                ++kept;
            }
        }

        return kept;
    }

    public static int flushLocalPinsToRemote(Path dir, String remote) {
        int kept = manageLocalPins(dir);
        if(kept == 0) {
            // the cached pins alone mean we're clean, bounce
            return 0;
        }
        // we think we have outstanding local pins, let's try a fetch and check again
        fetchPins(dir, remote);
        kept = manageLocalPins(dir);
        if(kept == 0) {
            // that covered it, great
            return 0;
        }
        // nope, we gotta push
        ProcessHelperUtils.runQuiet(dir, "git", "push", "-q", remote, "refs/qbt-local-pins/*:refs/qbt-pins/*");
        // count is not strictly correct on races but so be it
        return kept;
    }

    public static void publishBranch(Path dir, String remote, VcsVersionDigest commit, String name) {
        ProcessHelperUtils.runQuiet(dir, "git", "push", "-q", remote, "+" + commit.getRawDigest() + ":refs/heads/" + name);
    }

    public static void forcePush(Path dir, String remote, VcsVersionDigest from, String to) {
        ProcessHelperUtils.runQuiet(dir, "git", "push", "-q", remote, "+" + from.getRawDigest() + ":" + to);
    }

    public static void fetch(Path dir, String remote, String ref) {
        ProcessHelperUtils.runQuiet(dir, "git", "fetch", "-q", remote, ref);
    }

    public static Iterable<VcsVersionDigest> mergeBases(Path repo, VcsVersionDigest lhs, VcsVersionDigest rhs) {
        ProcessHelper p = new ProcessHelper(repo, "git", "merge-base", "-a", lhs.getRawDigest().toString(), rhs.getRawDigest().toString());
        p = p.inheritError();
        return Iterables.transform(p.completeLines(), new Function<String, VcsVersionDigest>() {
            @Override
            public VcsVersionDigest apply(String line) {
                return new VcsVersionDigest(QbtHashUtils.parse(line));
            }
        });
    }

    public static VcsTreeDigest getWorkingTreeTree(Path dir) {
        Path realIndexFile;
        {
            ProcessHelper p = new ProcessHelper(dir, "git", "rev-parse", "--git-dir");
            p = p.inheritError();
            realIndexFile = dir.resolve(p.completeLine()).resolve("index").toAbsolutePath();
        }

        try(QbtTempDir tempDir = new QbtTempDir()) {
            Path tempIndexFile = tempDir.resolve("temp.index");
            try(InputStream is = QbtUtils.openRead(realIndexFile); OutputStream os = QbtUtils.openWrite(tempIndexFile)) {
                ByteStreams.copy(is, os);
            }
            ProcessHelperUtils.runQuiet(dir, ImmutableMap.of("GIT_INDEX_FILE", tempIndexFile.toString()), "git", "add", "-A", ".");
            return getSubIndex(dir, tempIndexFile);
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static VcsVersionDigest revParse(Path dir, String arg) {
        return new VcsVersionDigest(new ProcessHelper(dir, "git", "rev-parse", arg).inheritError().completeSha1());
    }

    public static Multimap<String, String> getAllConfig(Path dir) {
        final ImmutableMultimap.Builder<String, String> b = ImmutableMultimap.builder();
        new ProcessHelper(dir, "git", "config", "-l").inheritError().completeLinesCallback(new Function<String, Void>() {
            @Override
            public Void apply(String line) {
                int i = line.indexOf('=');
                if(i == -1) {
                    throw new IllegalStateException("Bogus git config -l line: " + line);
                }
                b.put(line.substring(0, i), line.substring(i + 1));
                return null;
            }
        });
        return b.build();
    }

    public static VcsVersionDigest fetchAndResolveRemoteBranch(Path dir, String remote, String branch) {
        ProcessHelperUtils.runQuiet(dir, "git", "fetch", remote, "refs/heads/" + branch);
        return revParse(dir, "FETCH_HEAD");
    }

    public static String getCurrentBranch(Path dir) {
        Pair<ImmutableList<String>, Integer> ret = new ProcessHelper(dir, "git", "symbolic-ref", "HEAD").ignoreError().completeLinesAndExitCode();
        if(ret.getRight() != 0) {
            return null;
        }
        String value = Iterables.getOnlyElement(ret.getLeft());
        String prefix = "refs/heads/";
        if(!value.startsWith(prefix)) {
            return null;
        }
        return value.substring(prefix.length());
    }

    public static Multimap<String, String> getBranchConfig(Path dir, String currentBranch) {
        ImmutableMultimap.Builder<String, String> b = ImmutableMultimap.builder();
        String prefix = "branch." + currentBranch + ".";
        for(Map.Entry<String, String> e : getAllConfig(dir).entries()) {
            if(e.getKey().startsWith(prefix)) {
                b.put(e.getKey().substring(prefix.length()), e.getValue());
            }
        }
        return b.build();
    }

    public static void addConfigItem(Path dir, String key, String value) {
        ProcessHelperUtils.runQuiet(dir, "git", "config", "--add", key, value);
    }

    public static Iterable<String> getChangedPaths(Path dir, VcsVersionDigest lhs, VcsVersionDigest rhs) {
        return new ProcessHelper(dir, "git", "diff", "--name-only", lhs.getRawDigest().toString(), rhs.getRawDigest().toString()).inheritError().completeLines();
    }

    public static VcsVersionDigest getFirstParent(Path repoDir, VcsVersionDigest after) {
        return new VcsVersionDigest(new ProcessHelper(repoDir, "git", "rev-parse", after.getRawDigest() + "^").inheritError().completeSha1());
    }

    public static List<VcsVersionDigest> mergeBaseIndependent(Path repo, Collection<VcsVersionDigest> subCommitParents) {
        if(subCommitParents.isEmpty()) {
            return ImmutableList.of();
        }
        ImmutableList.Builder<String> commandBuilder = ImmutableList.builder();
        commandBuilder.add("git", "merge-base", "--independent");
        commandBuilder.addAll(Iterables.transform(subCommitParents, VcsVersionDigest.DEPARSE_FUNCTION));
        return ImmutableList.copyOf(Iterables.transform(new ProcessHelper(repo, commandBuilder.build().toArray(new String[0])).inheritError().completeLines(), VcsVersionDigest.PARSE_FUNCTION));
    }

    public static String getCommitterEmail(Path repo, VcsVersionDigest commit) {
        return Iterables.getOnlyElement(new ProcessHelper(repo, "git", "log", "-1", "--format=%cE", commit.getRawDigest().toString()).inheritError().completeLines());
    }
    public static String getCommitterName(Path repo, VcsVersionDigest commit) {
        return Iterables.getOnlyElement(new ProcessHelper(repo, "git", "log", "-1", "--format=%cN", commit.getRawDigest().toString()).inheritError().completeLines());
    }
    public static String getAuthorEmail(Path repo, VcsVersionDigest commit) {
        return Iterables.getOnlyElement(new ProcessHelper(repo, "git", "log", "-1", "--format=%aE", commit.getRawDigest().toString()).inheritError().completeLines());
    }
    public static String getAuthorName(Path repo, VcsVersionDigest commit) {
        return Iterables.getOnlyElement(new ProcessHelper(repo, "git", "log", "-1", "--format=%aN", commit.getRawDigest().toString()).inheritError().completeLines());
    }

    public static void createBranch(Path dir, String name, VcsVersionDigest commit) {
        ProcessHelperUtils.runQuiet(dir, "git", "branch", name, commit.getRawDigest().toString());
    }

    public static void rsyncBranches(Path dir, String localPrefix, String remote, String remotePrefix) {
        ProcessHelperUtils.runQuiet(dir, "git", "push", "--prune", "--force", remote, "refs/heads/" + localPrefix + "*:refs/heads/" + remotePrefix + "*");
    }

    public static VcsVersionDigest commitAll(Path dir, String message) {
        ProcessHelperUtils.runQuiet(dir, "git", "add", "-A");
        ProcessHelperUtils.runQuiet(dir, "git", "commit", "-m" + message);
        return getCurrentCommit(dir);
    }

    public static VcsVersionDigest commitCrosswindSquash(Path dir, List<VcsVersionDigest> onto, String message) {
        if(!isHeadClean(dir)) {
            throw new IllegalArgumentException();
        }
        VcsVersionDigest currentCommit = getCurrentCommit(dir);
        VcsTreeDigest tree = getSubtree(dir, currentCommit, "");
        ImmutableList.Builder<String> command = ImmutableList.builder();
        command.add("git", "commit-tree");
        for(VcsVersionDigest parent : onto) {
            command.add("-p", parent.getRawDigest().toString());
        }
        command.add("-m", message);
        command.add(tree.getRawDigest().toString());
        VcsVersionDigest commit = new VcsVersionDigest(QbtHashUtils.parse(new ProcessHelper(dir, command.build().toArray(new String[0])).inheritError().completeLine()));
        checkout(dir, commit);
        return commit;
    }

    public static VcsVersionDigest commitAllAmend(Path dir, String message) {
        ProcessHelperUtils.runQuiet(dir, "git", "add", "-A");
        ProcessHelperUtils.runQuiet(dir, "git", "commit", "--amend", "-m" + message);
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
                    b.put(currentCommit, new CommitData(currentTree, currentParents.build(), authorName, authorEmail, authorDate, committerName, committerEmail, committerDate, currentMessage.toString()));
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
        ProcessHelper p = new ProcessHelper(dir, args);
        p = p.inheritError();
        for(String line : p.completeLines()) {
            pp.parseLine(line);
        }
        return pp.eof();
    }

    public static VcsVersionDigest createCommit(Path dir, CommitData commitData) {
        ImmutableList.Builder<String> commitTreeCommand = ImmutableList.builder();
        commitTreeCommand.add("git", "commit-tree", "-m", commitData.message);
        for(VcsVersionDigest parent : commitData.parents){
            commitTreeCommand.add("-p", parent.getRawDigest().toString());
        }
        commitTreeCommand.add(commitData.tree.getRawDigest().toString());
        ProcessHelper p = new ProcessHelper(dir, commitTreeCommand.build().toArray(new String[0]));
        p = p.putEnv("GIT_AUTHOR_NAME", commitData.authorName);
        p = p.putEnv("GIT_AUTHOR_EMAIL", commitData.authorEmail);
        p = p.putEnv("GIT_AUTHOR_DATE", commitData.authorDate);
        p = p.putEnv("GIT_COMMITTER_NAME", commitData.committerName);
        p = p.putEnv("GIT_COMMITTER_EMAIL", commitData.committerEmail);
        p = p.putEnv("GIT_COMMITTER_DATE", commitData.committerDate);
        HashCode object = p.inheritError().completeSha1();
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
            return new ProcessHelper(dir, "git", "hash-object", "-w", tempFile.toString()).inheritError().completeSha1();
        }
    }

    public static byte[] readObject(Path dir, HashCode object) {
        try(QbtTempDir tempDir = new QbtTempDir()) {
            Path tempFile = tempDir.resolve("object");
            new ProcessHelper(dir, "git", "show", object.toString()).fileOutput(tempFile).inheritError().completeVoid();
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
}
