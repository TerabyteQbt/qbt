package qbt.vcs;

import com.google.common.collect.ImmutableList;
import java.util.List;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;

public final class CommitData extends Struct<CommitData> {
    private CommitData(ImmutableSalvagingMap<StructKey<CommitData, ?>, Object> map) {
        super(TYPE, map);
    }

    public static final StructKey<CommitData, VcsTreeDigest> TREE;
    public static final StructKey<CommitData, List<VcsVersionDigest>> PARENTS;
    public static final StructKey<CommitData, String> AUTHOR_NAME;
    public static final StructKey<CommitData, String> AUTHOR_EMAIL;
    public static final StructKey<CommitData, String> AUTHOR_DATE;
    public static final StructKey<CommitData, String> COMMITTER_NAME;
    public static final StructKey<CommitData, String> COMMITTER_EMAIL;
    public static final StructKey<CommitData, String> COMMITTER_DATE;
    public static final StructKey<CommitData, String> MESSAGE;
    public static final StructType<CommitData> TYPE;
    static {
        ImmutableList.Builder<StructKey<CommitData, ?>> b = ImmutableList.builder();

        b.add(TREE = new StructKey<CommitData, VcsTreeDigest>("tree"));
        b.add(PARENTS = new StructKey<CommitData, List<VcsVersionDigest>>("parents"));
        b.add(AUTHOR_NAME = new StructKey<CommitData, String>("authorName"));
        b.add(AUTHOR_EMAIL = new StructKey<CommitData, String>("authorEmail"));
        b.add(AUTHOR_DATE = new StructKey<CommitData, String>("authorDate"));
        b.add(COMMITTER_NAME = new StructKey<CommitData, String>("committerName"));
        b.add(COMMITTER_EMAIL = new StructKey<CommitData, String>("committerEmail"));
        b.add(COMMITTER_DATE = new StructKey<CommitData, String>("committerDate"));
        b.add(MESSAGE = new StructKey<CommitData, String>("message"));

        TYPE = new StructType<CommitData>(b.build()) {
            @Override
            protected CommitData create(ImmutableSalvagingMap<StructKey<CommitData, ?>, Object> map) {
                return new CommitData(map);
            }
        };
    }
}
