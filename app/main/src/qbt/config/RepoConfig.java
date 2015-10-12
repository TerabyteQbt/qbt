package qbt.config;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import qbt.PackageTip;
import qbt.VcsVersionDigest;
import qbt.repo.CommonRepoAccessor;
import qbt.vcs.CachedRemote;
import qbt.vcs.LocalVcs;

public final class RepoConfig {
    private final ImmutableList<RepoConfigEntry> entries;

    public RepoConfig(ImmutableList<RepoConfigEntry> entries) {
        this.entries = entries;
    }

    public CommonRepoAccessor requireRepo(PackageTip repo, VcsVersionDigest version) {
        for(RepoConfigEntry e : entries) {
            CommonRepoAccessor r = e.findRepo(repo, version);
            if(r != null) {
                return r;
            }
        }
        throw new IllegalArgumentException("Could not find repo for " + repo);
    }

    public interface RequireRepoRemoteResult {
        public CachedRemote getRemote();
        public CachedRemote getVanityRemote();
        public Path getLocalDirectory();
    }

    public RequireRepoRemoteResult requireRepoRemote(PackageTip repo, VcsVersionDigest version) {
        for(RepoConfigEntry e : entries) {
            RequireRepoRemoteResult r = e.findRepoRemote(repo, version);
            if(r != null) {
                return r;
            }
        }
        throw new IllegalArgumentException("Could not find remote repo for " + repo);
    }

    public interface RequireRepoLocalResult {
        LocalVcs getLocalVcs();
        Path getDirectory();
    }

    public RequireRepoLocalResult findLocalRepo(PackageTip repo) {
        for(RepoConfigEntry e : entries) {
            RequireRepoLocalResult r = e.findRepoLocal(repo);
            if(r != null) {
                return r;
            }
        }
        return null;
    }
}
