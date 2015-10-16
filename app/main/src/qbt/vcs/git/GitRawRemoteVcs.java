package qbt.vcs.git;

import java.nio.file.Path;
import qbt.VcsVersionDigest;
import qbt.vcs.LocalVcs;
import qbt.vcs.simple.SimpleRawRemoteVcs;

public final class GitRawRemoteVcs extends SimpleRawRemoteVcs {
    @Override
    public LocalVcs getLocalVcs() {
        return new GitLocalVcs();
    }

    @Override
    public void addRemote(Path dir, String name, String remote) {
        GitUtils.addRemote(dir, name, remote);
    }

    @Override
    public void fetchRemote(Path dir, String name) {
        GitUtils.fetchRemote(dir, name);
    }

    @Override
    public boolean isRemoteRaw(String remote) {
        return GitUtils.remoteExists(remote);
    }

    @Override
    public void fetchPins(Path dir, String remote) {
        GitUtils.fetchPins(dir, remote);
    }

    @Override
    public void addPinToRemote(Path dir, String remote, VcsVersionDigest commit) {
        GitUtils.addPinToRemote(dir, remote, commit);
    }

    @Override
    public void addLocalPinToRemote(Path dir, String remote, VcsVersionDigest commit) {
        GitUtils.addLocalPinToRemote(dir, remote, commit);
    }

    @Override
    public int flushLocalPinsToRemote(Path dir, String remote) {
        return GitUtils.flushLocalPinsToRemote(dir, remote);
    }

    @Override
    public void publishBranch(Path dir, String remote, VcsVersionDigest commit, String name) {
        GitUtils.publishBranch(dir, remote, commit, name);
    }

    @Override
    public String getName() {
        return "git";
    }

    @Override
    public void rsyncBranches(Path dir, String localPrefix, String remote, String remotePrefix) {
        GitUtils.rsyncBranches(dir, localPrefix, remote, remotePrefix);
    }

    @Override
    public boolean remoteExists(String remote) {
        return GitUtils.remoteExists(remote);
    }
}
