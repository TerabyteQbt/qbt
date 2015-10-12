package qbt.config;

import com.google.common.collect.ImmutableList;
import qbt.PackageTip;
import qbt.VcsVersionDigest;
import qbt.repo.RemoteRepoAccessor;

public final class RepoConfig {
    private final ImmutableList<RepoConfigEntry> entries;

    public RepoConfig(ImmutableList<RepoConfigEntry> entries) {
        this.entries = entries;
    }

    public RemoteRepoAccessor findRemoteRepo(PackageTip repo, VcsVersionDigest version) {
        for(RepoConfigEntry e : entries) {
            RemoteRepoAccessor r = e.findRemoteRepo(repo, version);
            if(r != null) {
                return r;
            }
        }
        return null;
    }

    public RemoteRepoAccessor requireRemoteRepo(PackageTip repo, VcsVersionDigest version) {
        RemoteRepoAccessor r = findRemoteRepo(repo, version);
        if(r == null) {
            throw new IllegalArgumentException("Could not find remote repo for " + repo + " at " + version);
        }
        return r;
    }
}
