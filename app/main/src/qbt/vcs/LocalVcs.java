package qbt.vcs;

import java.nio.file.Path;
import qbt.VcsTreeDigest;

public interface LocalVcs {
    public String getName();
    public boolean isRepo(Path dir);
    public VcsTreeDigest emptyTree();

    public Repository createWorkingRepo(Path dir);
    public Repository createCacheRepo(Path dir);
    public Repository getRepository(Path dir);
}
