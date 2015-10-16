package qbt.remote;

import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;

public interface QbtRemote {
    public RawRemote findRemote(RepoTip repo, boolean autoVivify);
}
