package qbt.map;

import org.apache.commons.lang3.tuple.Triple;
import qbt.PackageManifest;
import qbt.PackageTip;
import qbt.QbtManifest;
import qbt.RepoManifest;
import qbt.VcsVersionDigest;
import qbt.config.QbtConfig;
import qbt.repo.CommonRepoAccessor;

public abstract class ManifestCumulativeVersionComputer<K> extends CumulativeVersionComputer<K> {
    private final QbtConfig config;
    private final QbtManifest manifest;

    @Override
    protected Triple<PackageTip, RepoManifest, PackageManifest> requireManifest(PackageTip packageTip) {
        PackageTip repo = manifest.packageToRepo.get(packageTip);
        if(repo == null) {
            throw new RuntimeException("No package [tip] " + packageTip + " in manifest");
        }
        RepoManifest repoManifest = manifest.repos.get(repo);
        PackageManifest packageManifest = repoManifest.packages.get(packageTip.pkg);
        return Triple.of(repo, repoManifest, packageManifest);
    }

    @Override
    protected CommonRepoAccessor requireRepo(PackageTip repo, VcsVersionDigest version) {
        return config.repoConfig.requireRepo(repo, version);
    }

    public ManifestCumulativeVersionComputer(QbtConfig config, QbtManifest manifest) {
        this.config = config;
        this.manifest = manifest;
    }
}
