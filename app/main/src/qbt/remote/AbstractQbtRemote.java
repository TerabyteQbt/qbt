package qbt.remote;

import qbt.remote.QbtRemote;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;

public abstract class AbstractQbtRemote implements QbtRemote {
    @Override
    public RawRemote requireRemote(RepoTip repo) {
        RawRemote r = findRemote(repo);
        if(r == null) {
            throw new IllegalArgumentException("Could not remote for " + repo);
        }
        return r;
    }
}
