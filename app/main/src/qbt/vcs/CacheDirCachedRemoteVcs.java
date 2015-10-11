package qbt.vcs;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import org.apache.commons.lang3.ObjectUtils;
import qbt.QbtHashUtils;
import qbt.QbtUtils;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;

public class CacheDirCachedRemoteVcs implements CachedRemoteVcs {
    private final RawRemoteVcs remoteVcs;
    private final Path rootCacheDir;

    public CacheDirCachedRemoteVcs(RawRemoteVcs remoteVcs, Path rootCacheDir) {
        this.remoteVcs = remoteVcs;
        this.rootCacheDir = rootCacheDir;
    }

    @Override
    public RawRemoteVcs getRawRemoteVcs() {
        return remoteVcs;
    }

    private Collection<VcsVersionDigest> checkCache(Path cache, Path dir, Collection<VcsVersionDigest> versions) {
        ImmutableList.Builder<VcsVersionDigest> missing = ImmutableList.builder();
        for(VcsVersionDigest version : versions) {
            if(remoteVcs.getLocalVcs().getRepository(cache).commitExists(version)) {
                remoteVcs.addPinToRemote(cache, dir.toAbsolutePath().toString(), version);
            }
            else {
                missing.add(version);
            }
        }
        return missing.build();
    }

    private Path cache(String remote) {
        HashCode remoteDigest = QbtHashUtils.of(remote);
        return rootCacheDir.resolve(remoteDigest.toString());
    }

    private Path materializeCache(String remote) {
        Path cache = cache(remote);

        QbtUtils.semiAtomicDirCache(cache, "", new Function<Path, ObjectUtils.Null>() {
            @Override
            public ObjectUtils.Null apply(Path cacheTemp) {
                remoteVcs.getLocalVcs().createCacheRepo(cacheTemp);
                return ObjectUtils.NULL;
            }
        });

        return cache;
    }

    private Repository materializeCacheForVersion(String remote, VcsVersionDigest version) {
        Path cache = materializeCache(remote);
        Repository repository = remoteVcs.getLocalVcs().getRepository(cache);
        if(!repository.commitExists(version)) {
            remoteVcs.fetchPins(cache, remote);
        }
        return repository;
    }

    @Override
    public void findCommit(Path dir, String remote, Collection<VcsVersionDigest> versions) {
        Path cache = materializeCache(remote);
        versions = checkCache(cache, dir, versions);
        if(versions.isEmpty()) {
            return;
        }
        remoteVcs.fetchPins(cache, remote);
        versions = checkCache(cache, dir, versions);
        if(versions.isEmpty()) {
            return;
        }
        throw new IllegalStateException("Could not find " + versions);
    }

    @Override
    public boolean isRemote(String remote) {
        Path cache = cache(remote);
        if(Files.isDirectory(cache)) {
            return true;
        }
        return remoteVcs.isRemoteRaw(remote);
    }

    @Override
    public boolean commitExists(String remote, VcsVersionDigest version) {
        return materializeCacheForVersion(remote, version).commitExists(version);
    }

    @Override
    public VcsTreeDigest getSubtree(String remote, VcsVersionDigest version, String subpath) {
        return materializeCacheForVersion(remote, version).getSubtree(version, subpath);
    }

    @Override
    public void checkoutTree(String remote, VcsVersionDigest version, String subpath, Path dir) {
        Repository cache = materializeCacheForVersion(remote, version);
        cache.checkoutTree(cache.getSubtree(version, subpath), dir);
    }

    @Override
    public void addPin(Path dir, String remote, VcsVersionDigest commit) {
        Path cache = materializeCache(remote);
        remoteVcs.addLocalPinToRemote(dir, cache.toAbsolutePath().toString(), commit);
    }

    @Override
    public int flushPins(String remote) {
        Path cache = materializeCache(remote);
        return remoteVcs.flushLocalPinsToRemote(cache, remote);
    }

    @Override
    public String toString() {
        return "[" + remoteVcs.getName() + " caching in " + rootCacheDir + "]";
    }
}
