package qbt.map;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.PackageManifest;
import qbt.PackageTip;
import qbt.QbtManifest;

public abstract class SimpleDependencyComputer<V> extends DependencyComputer<PackageManifest, V> {
    private final QbtManifest manifest;

    public SimpleDependencyComputer(QbtManifest manifest) {
        this.manifest = manifest;
    }

    @Override
    protected PackageManifest premap(PackageTip packageTip) {
        PackageTip repo = manifest.packageToRepo.get(packageTip);
        return manifest.repos.get(repo).packages.get(packageTip.pkg);
    }

    @Override
    protected Map<String, Pair<NormalDependencyType, String>> getNormalDeps(PackageManifest packageManifest, PackageTip packageTip) {
        return packageManifest.normalDeps;
    }

    @Override
    protected Map<PackageTip, String> getReplaceDeps(PackageManifest packageManifest, PackageTip packageTip) {
        return packageManifest.replaceDeps;
    }
}
