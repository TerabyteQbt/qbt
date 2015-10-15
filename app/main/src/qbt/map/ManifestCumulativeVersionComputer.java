package qbt.map;

import org.apache.commons.lang3.tuple.Triple;
import qbt.PackageManifest;
import qbt.QbtManifest;
import qbt.RepoManifest;
import qbt.VcsVersionDigest;
import qbt.config.QbtConfig;
import qbt.repo.CommonRepoAccessor;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public abstract class ManifestCumulativeVersionComputer<K> extends CumulativeVersionComputer<K> {
    private final QbtConfig config;
    private final QbtManifest manifest;

    @Override
    protected Triple<RepoTip, RepoManifest, PackageManifest> requireManifest(PackageTip packageTip) {
        RepoTip repo = manifest.packageToRepo.get(packageTip);
        if(repo == null) {
            throw new RuntimeException("No package [tip] " + packageTip + " in manifest");
        }
        RepoManifest repoManifest = manifest.repos.get(repo);
        PackageManifest packageManifest = repoManifest.packages.get(packageTip.name);
        return Triple.of(repo, repoManifest, packageManifest);
    }

    @Override
    protected CommonRepoAccessor requireRepo(RepoTip repo, VcsVersionDigest version) {
        return config.requireCommonRepo(repo, version);
    }

    public ManifestCumulativeVersionComputer(QbtConfig config, QbtManifest manifest) {
        this.config = config;
        this.manifest = manifest;
    }
}
