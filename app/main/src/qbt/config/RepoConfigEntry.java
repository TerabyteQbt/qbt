package qbt.config;

import com.google.common.collect.ImmutableList;
import qbt.PackageTip;
import qbt.VcsVersionDigest;
import qbt.repo.RemoteRepoAccessor;

public final class RepoConfigEntry {
    public final ImmutableList<RemoteRepoConfigEntry> remoteConfigs;

    public RepoConfigEntry(ImmutableList<RemoteRepoConfigEntry> remoteConfigs) {
        this.remoteConfigs = remoteConfigs;
    }

    public RemoteRepoAccessor findRemoteRepo(PackageTip repo, VcsVersionDigest version) {
        for(RemoteRepoConfigEntry remoteConfig : remoteConfigs) {
            RemoteRepoAccessor r = remoteConfig.findRemoteRepo(repo, version);
            if(r != null) {
                return r;
            }
        }

        return null;
    }
}
