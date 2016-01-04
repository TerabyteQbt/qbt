package qbt.vcs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.SimpleStructKey;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructBuilder;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;

public final class CommitData extends Struct<CommitData, CommitData.Builder> {
    private CommitData(ImmutableMap<StructKey<CommitData, ?, ?>, Object> map) {
        super(TYPE, map);
    }

    public static class Builder extends StructBuilder<CommitData, Builder> {
        private Builder(ImmutableSalvagingMap<StructKey<CommitData, ?, ?>, Object> map) {
            super(TYPE, map);
        }
    }

    public static final SimpleStructKey<CommitData, VcsTreeDigest> TREE;
    public static final SimpleStructKey<CommitData, ImmutableList<VcsVersionDigest>> PARENTS;
    public static final SimpleStructKey<CommitData, String> AUTHOR_NAME;
    public static final SimpleStructKey<CommitData, String> AUTHOR_EMAIL;
    public static final SimpleStructKey<CommitData, String> AUTHOR_DATE;
    public static final SimpleStructKey<CommitData, String> COMMITTER_NAME;
    public static final SimpleStructKey<CommitData, String> COMMITTER_EMAIL;
    public static final SimpleStructKey<CommitData, String> COMMITTER_DATE;
    public static final SimpleStructKey<CommitData, String> MESSAGE;
    public static final StructType<CommitData, Builder> TYPE;
    static {
        ImmutableList.Builder<StructKey<CommitData, ?, ?>> b = ImmutableList.builder();

        b.add(TREE = new SimpleStructKey<CommitData, VcsTreeDigest>("tree"));
        b.add(PARENTS = new SimpleStructKey<CommitData, ImmutableList<VcsVersionDigest>>("parents"));
        b.add(AUTHOR_NAME = new SimpleStructKey<CommitData, String>("authorName"));
        b.add(AUTHOR_EMAIL = new SimpleStructKey<CommitData, String>("authorEmail"));
        b.add(AUTHOR_DATE = new SimpleStructKey<CommitData, String>("authorDate"));
        b.add(COMMITTER_NAME = new SimpleStructKey<CommitData, String>("committerName"));
        b.add(COMMITTER_EMAIL = new SimpleStructKey<CommitData, String>("committerEmail"));
        b.add(COMMITTER_DATE = new SimpleStructKey<CommitData, String>("committerDate"));
        b.add(MESSAGE = new SimpleStructKey<CommitData, String>("message"));

        TYPE = new StructType<CommitData, Builder>(b.build(), CommitData::new, Builder::new);
    }
}
