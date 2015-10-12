package qbt.remote;

import qbt.PackageTip;
import qbt.remote.QbtRemote;
import qbt.vcs.RawRemote;

public abstract class AbstractQbtRemote implements QbtRemote {
    @Override
    public RawRemote requireRemote(PackageTip repo) {
        RawRemote r = findRemote(repo);
        if(r == null) {
            throw new IllegalArgumentException("Could not remote for " + repo);
        }
        return r;
    }
}
