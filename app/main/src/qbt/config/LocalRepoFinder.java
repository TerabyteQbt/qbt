package qbt.config;

import qbt.repo.LocalRepoAccessor;
import qbt.tip.RepoTip;

public interface LocalRepoFinder {
    LocalRepoAccessor findLocalRepo(RepoTip repo);
    LocalRepoAccessor createLocalRepo(RepoTip repo);
}
