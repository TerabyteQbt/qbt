package qbt.vcs;

import com.google.common.collect.ImmutableList;
import java.util.List;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;

public final class CommitData {
    public final VcsTreeDigest tree;
    public final List<VcsVersionDigest> parents;
    public final String authorName;
    public final String authorEmail;
    public final String authorDate;
    public final String committerName;
    public final String committerEmail;
    public final String committerDate;
    public final String message;

    public CommitData(VcsTreeDigest tree, List<VcsVersionDigest> parents, String authorName, String authorEmail, String authorDate, String committerName, String committerEmail, String committerDate, String message) {
        this.tree = tree;
        this.parents = ImmutableList.copyOf(parents);
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.authorDate = authorDate;
        this.committerName = committerName;
        this.committerEmail = committerEmail;
        this.committerDate = committerDate;
        this.message = message;
    }

    public CommitData withTree(VcsTreeDigest newTree) {
        return new CommitData(newTree, parents, authorName, authorEmail, authorDate, committerName, committerEmail, committerDate, message);
    }

    public CommitData withParents(List<VcsVersionDigest> newParents) {
        return new CommitData(tree, newParents, authorName, authorEmail, authorDate, committerName, committerEmail, committerDate, message);
    }

    public CommitData withMessage(String newMessage) {
        return new CommitData(tree, parents, authorName, authorEmail, authorDate, committerName, committerEmail, committerDate, newMessage);
    }
}
