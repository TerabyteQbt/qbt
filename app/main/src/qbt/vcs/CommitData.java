package qbt.vcs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructBuilder;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import misc1.commons.ds.StructTypeBuilder;
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

    public static final StructKey<CommitData, VcsTreeDigest, VcsTreeDigest> TREE;
    public static final StructKey<CommitData, ImmutableList<VcsVersionDigest>, ImmutableList<VcsVersionDigest>> PARENTS;
    public static final StructKey<CommitData, String, String> AUTHOR_NAME;
    public static final StructKey<CommitData, String, String> AUTHOR_EMAIL;
    public static final StructKey<CommitData, String, String> AUTHOR_DATE;
    public static final StructKey<CommitData, String, String> COMMITTER_NAME;
    public static final StructKey<CommitData, String, String> COMMITTER_EMAIL;
    public static final StructKey<CommitData, String, String> COMMITTER_DATE;
    public static final StructKey<CommitData, String, String> MESSAGE;
    public static final StructType<CommitData, Builder> TYPE;
    static {
        StructTypeBuilder<CommitData, Builder> b = new StructTypeBuilder<>(CommitData::new, Builder::new);

        TREE = b.<VcsTreeDigest>key("tree").add();
        PARENTS = b.<ImmutableList<VcsVersionDigest>>key("parents").add();
        AUTHOR_NAME = b.<String>key("authorName").add();
        AUTHOR_EMAIL = b.<String>key("authorEmail").add();
        AUTHOR_DATE = b.<String>key("authorDate").add();
        COMMITTER_NAME = b.<String>key("committerName").add();
        COMMITTER_EMAIL = b.<String>key("committerEmail").add();
        COMMITTER_DATE = b.<String>key("committerDate").add();
        MESSAGE = b.<String>key("message").add();

        TYPE = b.build();
    }
}
