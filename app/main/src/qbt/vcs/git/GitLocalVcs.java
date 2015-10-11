package qbt.vcs.git;

import java.nio.file.Files;
import java.nio.file.Path;
import qbt.QbtUtils;
import qbt.VcsTreeDigest;
import qbt.vcs.Repository;
import qbt.vcs.simple.SimpleLocalVcs;

public final class GitLocalVcs extends SimpleLocalVcs {
    @Override
    public String getName() {
        return "git";
    }

    @Override
    public boolean isRepo(Path dir) {
        return Files.isDirectory(dir.resolve(".git"));
    }

    private void checkEmpty(Path dir) {
        if(Files.isDirectory(dir) && QbtUtils.listChildren(dir).size() > 0) {
            throw new RuntimeException("Path already exists: " + dir);
        }
    }


    @Override
    public Repository getRepository(Path dir) {
        return new GitRepository(dir);
    }

    @Override
    public Repository createWorkingRepo(Path dir) {
        checkEmpty(dir);
        GitUtils.createWorkingRepo(dir);
        return new GitRepository(dir);
    }

    @Override
    public Repository createCacheRepo(Path dir) {
        checkEmpty(dir);
        GitUtils.createCacheRepo(dir);
        return new GitRepository(dir);
    }

    @Override
    public VcsTreeDigest emptyTree() {
        return GitUtils.EMPTY_TREE;
    }

}
