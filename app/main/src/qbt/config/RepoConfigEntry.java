package qbt.config;

import com.google.common.collect.ImmutableList;
import qbt.PackageTip;
import qbt.VcsVersionDigest;

public final class RepoConfigEntry {
    public final LocalRepoConfigEntry localConfig;
    public final ImmutableList<RemoteRepoConfigEntry> remoteConfigs;

    public RepoConfigEntry(LocalRepoConfigEntry localConfig, ImmutableList<RemoteRepoConfigEntry> remoteConfigs) {
        this.localConfig = localConfig;
        this.remoteConfigs = remoteConfigs;
    }

    public RepoConfig.RequireRepoResult findRepo(PackageTip repo, VcsVersionDigest version) {
        if(localConfig != null) {
            RepoConfig.RequireRepoResult r = localConfig.findRepo(repo, version);
            if(r != null) {
                return r;
            }
        }

        for(RemoteRepoConfigEntry remoteConfig : remoteConfigs) {
            RepoConfig.RequireRepoResult r = remoteConfig.findRepo(repo, version);
            if(r != null) {
                return r;
            }
        }

        return null;
    }

    public RepoConfig.RequireRepoRemoteResult findRepoRemote(PackageTip repo, VcsVersionDigest version) {
        for(RemoteRepoConfigEntry remoteConfig : remoteConfigs) {
            RepoConfig.RequireRepoRemoteResult r = remoteConfig.findRepoRemote(localConfig, repo, version);
            if(r != null) {
                return r;
            }
        }

        return null;
    }

    public RepoConfig.RequireRepoLocalResult findRepoLocal(PackageTip repo) {
        if(localConfig != null) {
            return localConfig.findRepoLocal(repo);
        }
        return null;
    }
}
