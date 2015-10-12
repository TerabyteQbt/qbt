package qbt.config;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.PackageTip;
import qbt.VcsVersionDigest;
import qbt.repo.CommonRepoAccessor;
import qbt.repo.RemoteRepoAccessor;
import qbt.vcs.CachedRemote;
import qbt.vcs.CachedRemoteVcs;

public final class RemoteRepoConfigEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteRepoConfigEntry.class);

    private final CachedRemoteVcs remoteVcs;
    private final String format;
    private final Map<String, CachedRemote> vanityRemotes;

    public RemoteRepoConfigEntry(CachedRemoteVcs remoteVcs, String format) {
        this(remoteVcs, format, ImmutableMap.<String, CachedRemote>of());
    }

    public RemoteRepoConfigEntry(CachedRemoteVcs remoteVcs, String format, Map<String, CachedRemote> vanityRemotes) {
        this.remoteVcs = remoteVcs;
        this.format = format;
        this.vanityRemotes = ImmutableMap.copyOf(vanityRemotes);
    }

    private String find(PackageTip repo, VcsVersionDigest version) {
        String remote = format.replace("%r", repo.pkg);

        if(!remoteVcs.isRemote(remote)) {
            LOGGER.debug("Remote repo check for " + repo + " at " + remote + " missed (not remote)");
            return null;
        }
        if(!remoteVcs.commitExists(remote, version)) {
            LOGGER.debug("Remote repo check for " + repo + " at " + remote + " missed (doesn't have " + version + ")");
            return null;
        }
        LOGGER.debug("Remote repo check for " + repo + " at " + remote + " hit");
        return remote;
    }

    public CommonRepoAccessor findRepo(PackageTip repo, VcsVersionDigest version) {
        final String remote = find(repo, version);
        if(remote == null) {
            return null;
        }
        return new RemoteRepoAccessor(remoteVcs, remote, version);
    }

    public RepoConfig.RequireRepoRemoteResult findRepoRemote(final LocalRepoConfigEntry localConfig, final PackageTip repo, final VcsVersionDigest version) {
        final String remote = find(repo, version);
        if(remote == null) {
            return null;
        }
        return new RepoConfig.RequireRepoRemoteResult() {
            @Override
            public CachedRemote getRemote() {
                return new CachedRemote(remote, remoteVcs);
            }

            @Override
            public Path getLocalDirectory() {
                if(localConfig == null) {
                    return null;
                }
                return localConfig.formatDirectory(repo);
            }
        };
    }
}
