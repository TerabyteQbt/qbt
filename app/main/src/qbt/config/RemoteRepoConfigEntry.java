package qbt.config;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.util.Map;
import misc1.commons.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.PackageDirectory;
import qbt.PackageTip;
import qbt.QbtTempDir;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.repo.CommonRepoAccessor;
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

    public CommonRepoAccessor findRepo(PackageTip repo, final VcsVersionDigest version) {
        final String remote = find(repo, version);
        if(remote == null) {
            return null;
        }
        return new CommonRepoAccessor() {
            @Override
            public PackageDirectory makePackageDirectory(String prefix) {
                final QbtTempDir packageDir = new QbtTempDir();
                // We could leak packageDir if this checkout crashes but oh
                // well.
                remoteVcs.checkoutTree(remote, version, prefix, packageDir.path);
                return new PackageDirectory() {
                    @Override
                    public Path getDir() {
                        return packageDir.path;
                    }

                    @Override
                    public void close() {
                        packageDir.close();
                    }
                };
            }

            @Override
            public VcsTreeDigest getEffectiveTree(Maybe<String> prefix) {
                if(prefix.isPresent()) {
                    return remoteVcs.getSubtree(remote, version, prefix.get(null));
                }
                else {
                    return remoteVcs.getRawRemoteVcs().getLocalVcs().emptyTree();
                }
            }

            @Override
            public boolean isOverride() {
                return false;
            }
        };
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
            public CachedRemote getVanityRemote() {
                CachedRemote ret = vanityRemotes.get(repo.pkg);
                if(ret == null) {
                    ret = getRemote();
                }
                return ret;
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
