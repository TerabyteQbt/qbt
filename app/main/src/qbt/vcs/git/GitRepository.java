package qbt.vcs.git;

import com.google.common.collect.Multimap;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.vcs.CommitData;
import qbt.vcs.CommitLevel;
import qbt.vcs.Repository;
import qbt.vcs.TreeAccessor;

public class GitRepository implements Repository {

    private final Path repositoryPath;

    public GitRepository(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    @Override
    public void checkout(VcsVersionDigest version) {
        GitUtils.checkout(repositoryPath, version);
    }

    @Override
    public boolean isAncestorOf(VcsVersionDigest ancestor, VcsVersionDigest descendent) {
        return GitUtils.isAncestorOf(repositoryPath, ancestor, descendent);
    }

    @Override
    public void merge(VcsVersionDigest commit) {
        GitUtils.merge(repositoryPath, commit);
    }

    @Override
    public void rebase(VcsVersionDigest from, VcsVersionDigest to) {
        GitUtils.rebase(repositoryPath, from, to);
    }

    @Override
    public VcsVersionDigest getCurrentCommit() {
        return GitUtils.getCurrentCommit(repositoryPath);
    }

    @Override
    public Iterable<String> showFile(VcsVersionDigest commit, String path) {
        return GitUtils.showFile(repositoryPath, commit, path);
    }

    @Override
    public Iterable<String> showFile(VcsTreeDigest tree, String path) {
        return GitUtils.showFile(repositoryPath, tree, path);
    }

    @Override
    public Iterable<VcsVersionDigest> mergeBases(VcsVersionDigest lhs, VcsVersionDigest rhs) {
        return GitUtils.mergeBases(repositoryPath, lhs, rhs);
    }

    @Override
    public boolean commitExists(VcsVersionDigest version) {
        return GitUtils.objectExists(repositoryPath, version);
    }

    @Override
    public Multimap<String, String> getAllConfig() {
        return GitUtils.getAllConfig(repositoryPath);
    }

    @Override
    public boolean isClean() {
        return GitUtils.isClean(repositoryPath);
    }

    @Override
    public boolean isClean(CommitLevel level) {
        return GitUtils.isClean(repositoryPath, level);
    }

    @Override
    public Path getRoot() {
        return GitUtils.getRoot(repositoryPath);
    }

    @Override
    public Iterable<String> getChangedPaths(VcsVersionDigest lhs, VcsVersionDigest rhs) {
        return GitUtils.getChangedPaths(repositoryPath, lhs, rhs);
    }

    @Override
    public VcsTreeDigest getSubtree(VcsVersionDigest version, String subpath) {
        return GitUtils.getSubtree(repositoryPath, version, subpath);
    }

    @Override
    public VcsTreeDigest getSubtree(VcsTreeDigest tree, String subpath) {
        return GitUtils.getSubtree(repositoryPath, tree, subpath);
    }

    @Override
    public void checkoutTree(VcsTreeDigest tree, Path dest) {
        GitUtils.checkoutTree(repositoryPath, tree, dest);
    }

    @Override
    public void checkout(String localBranchName) {
        GitUtils.checkout(repositoryPath, localBranchName);
    }

    @Override
    public void addConfigItem(String key, String value) {
        GitUtils.addConfigItem(repositoryPath, key, value);
    }

    @Override
    public String toString() {
        return "[Git repository at " + repositoryPath + "]";
    }

    @Override
    public VcsTreeDigest getEffectiveTree(String subpath) {
        // fast common case
        if(GitUtils.isClean(repositoryPath.resolve(subpath))) {
            return getSubtree(getCurrentCommit(), subpath);
        }
        return GitUtils.getWorkingTree(repositoryPath.resolve(subpath), CommitLevel.UNTRACKED);
    }

    @Override
    public void createBranch(String name, VcsVersionDigest commit) {
        GitUtils.createBranch(repositoryPath, name, commit);
    }

    @Override
    public VcsVersionDigest commit(boolean amend, String message, CommitLevel level) {
        return GitUtils.commit(repositoryPath, amend, message, level);
    }

    @Override
    public Map<VcsVersionDigest, CommitData> revWalk(Collection<VcsVersionDigest> from, Collection<VcsVersionDigest> to) {
        return GitUtils.revWalk(repositoryPath, from, to);
    }

    @Override
    public CommitData getCommitData(VcsVersionDigest commit) {
        return GitUtils.getCommitData(repositoryPath, commit);
    }

    @Override
    public VcsVersionDigest getUserSpecifiedCommit(String arg) {
        return GitUtils.revParse(repositoryPath, arg);
    }

    @Override
    public List<String> getUserVisibleStatus() {
        return GitUtils.getUserVisibleStatus(repositoryPath);
    }

    @Override
    public VcsVersionDigest createCommit(CommitData commitData) {
        return GitUtils.createCommit(repositoryPath, commitData);
    }

    @Override
    public TreeAccessor getTreeAccessor(VcsTreeDigest tree) {
        return new ColdGitTreeAccessor(repositoryPath, tree);
    }
}
