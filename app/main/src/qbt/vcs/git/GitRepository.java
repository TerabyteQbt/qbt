package qbt.vcs.git;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.vcs.CommitData;
import qbt.vcs.Repository;

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
    public String getCurrentBranch() {
        return GitUtils.getCurrentBranch(repositoryPath);
    }

    @Override
    public Iterable<String> showFile(VcsVersionDigest commit, String path) {
        return GitUtils.showFile(repositoryPath, commit, path);
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
    public Multimap<String, String> getCurrentBranchConfig() {
        String currentBranch = GitUtils.getCurrentBranch(repositoryPath);
        if(currentBranch == null) {
            return ImmutableMultimap.of();
        }
        return GitUtils.getBranchConfig(repositoryPath, currentBranch);
    }

    @Override
    public boolean isClean() {
        return GitUtils.isHeadClean(repositoryPath);
    }

    @Override
    public Path getRoot() {
        return GitUtils.getRoot(repositoryPath);
    }

    @Override
    public VcsVersionDigest fetchAndResolveRemoteBranch(String remote, String branch) {
        return GitUtils.fetchAndResolveRemoteBranch(repositoryPath, remote, branch);
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
    public VcsTreeDigest getEffectiveTree(Path subpath) {
        return GitUtils.getWorkingTreeTree(repositoryPath.resolve(subpath));
    }

    @Override
    public void createBranch(String name, VcsVersionDigest commit) {
        GitUtils.createBranch(repositoryPath, name, commit);
    }

    @Override
    public VcsVersionDigest commitAll(String message) {
        return GitUtils.commitAll(repositoryPath, message);
    }

    @Override
    public VcsVersionDigest commitCrosswindSquash(List<VcsVersionDigest> onto, String message) {
        return GitUtils.commitCrosswindSquash(repositoryPath, onto, message);
    }

    @Override
    public VcsVersionDigest commitAllAmend(String message) {
        return GitUtils.commitAllAmend(repositoryPath, message);
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
    public VcsVersionDigest createCommit(CommitData commitData) {
        return GitUtils.createCommit(repositoryPath, commitData);
    }
}
