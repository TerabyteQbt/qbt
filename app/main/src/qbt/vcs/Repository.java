package qbt.vcs;

import com.google.common.collect.Multimap;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;

public interface Repository {
    // Various not-very-horrible informational stuff
    public VcsTreeDigest getEffectiveTree(Path subpath);
    public boolean isAncestorOf(VcsVersionDigest ancestor, VcsVersionDigest descendent);
    public VcsVersionDigest getCurrentCommit();
    public String getCurrentBranch();
    public Iterable<String> showFile(VcsVersionDigest commit, String path);
    public Iterable<VcsVersionDigest> mergeBases(VcsVersionDigest lhs, VcsVersionDigest rhs);
    public boolean commitExists(VcsVersionDigest version);
    public Multimap<String, String> getAllConfig();
    public Multimap<String, String> getCurrentBranchConfig();
    public boolean isClean();
    public Path getRoot();
    public Iterable<String> getChangedPaths(VcsVersionDigest lhs, VcsVersionDigest rhs);
    public VcsTreeDigest getSubtree(VcsVersionDigest version, String subpath);
    public Map<VcsVersionDigest, CommitData> revWalk(Collection<VcsVersionDigest> from, Collection<VcsVersionDigest> to);
    public CommitData getCommitData(VcsVersionDigest commit);
    public VcsVersionDigest getUserSpecifiedCommit(String arg);

    // Various semi-crummy mutators invented for various semi-crummy needs
    public void checkout(VcsVersionDigest version);
    public void checkout(String localBranchName);
    public VcsVersionDigest fetchAndResolveRemoteBranch(String remote, String branch);
    public void checkoutTree(VcsTreeDigest tree, Path dest);
    public void addConfigItem(String key, String value);
    public void createBranch(String name, VcsVersionDigest commit);
    public VcsVersionDigest commitAll(String message);
    public VcsVersionDigest commitCrosswindSquash(List<VcsVersionDigest> onto, String message);
    public VcsVersionDigest commitAllAmend(String message);

    // before: repo is somewhere, clean
    // after: [failure] throw, repo state unclear
    // after: [success] repo is at merge of previous place and commit, clean
    public void merge(VcsVersionDigest commit);

    // before: repo is somewhere, clean
    // after: [failure] throw, repo state unclear
    // after: [success] repo is at rebuild on previous place of [linear] from..to, clean
    public void rebase(VcsVersionDigest from, VcsVersionDigest to);

    // create commit in the background
    public VcsVersionDigest createCommit(CommitData commitData);
}
