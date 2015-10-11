package qbt.config;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import misc1.commons.Maybe;
import qbt.PackageDirectory;
import qbt.PackageTip;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.vcs.CachedRemote;
import qbt.vcs.LocalVcs;

public final class RepoConfig {
    private final ImmutableList<RepoConfigEntry> entries;

    public RepoConfig(ImmutableList<RepoConfigEntry> entries) {
        this.entries = entries;
    }

    public interface RequireRepoResult {
        public PackageDirectory makePackageDirectory(String prefix);
        public VcsTreeDigest getEffectiveTree(Maybe<String> prefix);
        public boolean isOverride();
    }

    public RequireRepoResult requireRepo(PackageTip repo, VcsVersionDigest version) {
        for(RepoConfigEntry e : entries) {
            RequireRepoResult r = e.findRepo(repo, version);
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
