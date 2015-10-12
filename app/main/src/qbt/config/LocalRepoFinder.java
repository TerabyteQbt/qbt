package qbt.config;

import qbt.PackageTip;
import qbt.repo.LocalRepoAccessor;

public interface LocalRepoFinder {
    public LocalRepoAccessor findLocalRepo(PackageTip repo);
    public LocalRepoAccessor createLocalRepo(PackageTip repo);
}
