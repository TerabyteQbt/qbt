package qbt.remote;

import qbt.PackageTip;
import qbt.vcs.RawRemote;

public interface QbtRemote {
    public RawRemote findRemote(PackageTip repo);
    public RawRemote requireRemote(PackageTip repo);
}
