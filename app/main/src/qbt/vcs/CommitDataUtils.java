package qbt.vcs;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import qbt.VcsVersionDigest;

public final class CommitDataUtils {
    private CommitDataUtils() {
        // no
    }

    public static String getOneLiner(Repository repo, VcsVersionDigest commit) {
        return getOneLiner(commit, repo.getCommitData(commit));
    }

    public static String getOneLiner(VcsVersionDigest commit, CommitData commitData) {
        return commit.getRawDigest().toString().substring(0, 12) + " " + getCommitSubject(commitData);
    }

    public static String getCommitSubject(CommitData commitData) {
        return Splitter.on('\n').split(commitData.get(CommitData.MESSAGE)).iterator().next();
    }

    public static Iterable<Pair<VcsVersionDigest, CommitData>> revWalkFlatten(final Map<VcsVersionDigest, CommitData> revWalk, Iterable<VcsVersionDigest> commits) {
        final ImmutableList.Builder<Pair<VcsVersionDigest, CommitData>> b = ImmutableList.builder();
        final Set<VcsVersionDigest> already = Sets.newHashSet();
        class Helper {
            public void build(VcsVersionDigest commit) {
                if(!already.add(commit)) {
                    return;
                }
                CommitData commitData = revWalk.get(commit);
                if(commitData == null) {
                    return;
                }
                for(VcsVersionDigest parent : commitData.get(CommitData.PARENTS)) {
                    build(parent);
                }
                b.add(Pair.of(commit, commitData));
            }
        }
        Helper h = new Helper();
        for(VcsVersionDigest commit : commits) {
            h.build(commit);
        }
        return b.build().reverse();
    }
}
