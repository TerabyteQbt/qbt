package qbt.vcs;

import com.google.common.collect.ImmutableList;
import java.util.List;
import qbt.VcsVersionDigest;

public final class CommitData {
    public final String message;
    public final String committerName;
    public final String committerEmail;
    public final List<VcsVersionDigest> parents;

    public CommitData(String message, String committerName, String committerEmail, List<VcsVersionDigest> parents) {
        this.message = message;
        this.committerName = committerName;
        this.committerEmail = committerEmail;
        this.parents = ImmutableList.copyOf(parents);
    }
}
