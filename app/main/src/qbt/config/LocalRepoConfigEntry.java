package qbt.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import misc1.commons.Maybe;
import misc1.commons.concurrent.treelock.ArrayTreeLock;
import misc1.commons.concurrent.treelock.ArrayTreeLockPath;
import misc1.commons.concurrent.treelock.SimpleTreeLockPath;
import misc1.commons.concurrent.treelock.TreeLock;
import misc1.commons.concurrent.treelock.TreeLockInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.PackageDirectory;
import qbt.PackageTip;
import qbt.VcsTreeDigest;
import qbt.VcsVersionDigest;
import qbt.repo.CommonRepoAccessor;
import qbt.vcs.LocalVcs;

public final class LocalRepoConfigEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRepoConfigEntry.class);

    public final LocalVcs localVcs;
    public final String format;

    public LocalRepoConfigEntry(LocalVcs localVcs, String format) {
        this.localVcs = localVcs;
        this.format = format;
    }

    public Path formatDirectory(PackageTip packageTip) {
        return Paths.get(format.replace("%r", packageTip.pkg).replace("%t", packageTip.tip));
    }

    public CommonRepoAccessor findRepo(final PackageTip repo, VcsVersionDigest version) {
        final Path repoDir = formatDirectory(repo);
        if(!localVcs.isRepo(repoDir)) {
            LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " missed");
            return null;
        }
        LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " hit");
        return new CommonRepoAccessor() {
            private Path packageDir(String prefix) {
                return prefix.isEmpty() ? repoDir : repoDir.resolve(prefix);
            }

            @Override
            public PackageDirectory makePackageDirectory(final String prefix) {
                lock(repo, prefix);
                return new PackageDirectory() {
                    @Override
                    public Path getDir() {
                        return packageDir(prefix);
                    }

                    @Override
                    public void close() {
                        unlock(repo, prefix);
                    }
                };
            }

            @Override
            public VcsTreeDigest getEffectiveTree(Maybe<String> prefix) {
                if(prefix.isPresent()) {
                    return localVcs.getRepository(repoDir).getEffectiveTree(Paths.get(prefix.get(null)));
                }
                else {
                    return localVcs.emptyTree();
                }
            }

            @Override
            public boolean isOverride() {
                return true;
            }
        };
    }

    public RepoConfig.RequireRepoLocalResult findRepoLocal(final PackageTip repo) {
        final Path repoDir = formatDirectory(repo);
        if(!localVcs.isRepo(repoDir)) {
            LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " missed");
            return null;
        }
        LOGGER.debug("Local repo check for " + repo + " at " + repoDir + " hit");
        return new RepoConfig.RequireRepoLocalResult() {
            @Override
            public LocalVcs getLocalVcs() {
                return localVcs;
            }

            @Override
            public Path getDirectory() {
                return repoDir;
            }
        };
    }

    private static final class MyTreeLock extends TreeLock<PackageTip, ArrayTreeLockPath<String>> {
        @Override
        protected TreeLockInterface<? super ArrayTreeLockPath<String>> newChild() {
            return new ArrayTreeLock<String>();
        }
    }
    private final MyTreeLock inUse = new MyTreeLock();

    private void lock(PackageTip repo, String prefix) {
        LOGGER.debug("Local repo locking " + repo + "/" + prefix);
        inUse.lock(SimpleTreeLockPath.of(repo, ArrayTreeLockPath.split(prefix, '/')));
    }

    private void unlock(PackageTip repo, String prefix) {
        LOGGER.debug("Local repo unlocking " + repo + "/" + prefix);
        inUse.unlock(SimpleTreeLockPath.of(repo, ArrayTreeLockPath.split(prefix, '/')));
    }
}
