package qbt.config;

import qbt.repo.LocalRepoAccessor;
import qbt.tip.RepoTip;

public interface LocalRepoFinder {
    public LocalRepoAccessor findLocalRepo(RepoTip repo);
    public LocalRepoAccessor createLocalRepo(RepoTip repo);
}
