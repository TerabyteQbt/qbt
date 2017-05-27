package qbt;

import qbt.build.BuildData;
import qbt.manifest.current.PackageMetadata;
import qbt.map.CumulativeVersionComputer;
import qbt.repo.CommonRepoAccessor;

public final class PackageDirectories {
    private PackageDirectories() {
        // no
    }

    public static PackageDirectory forBuildData(BuildData bd) {
        return forCommon(bd.metadata, bd.commonRepoAccessor);
    }

    public static PackageDirectory forCvcResult(CumulativeVersionComputer.Result result) {
        return forCommon(result.packageManifest.metadata, result.commonRepoAccessor);
    }

    private static PackageDirectory forCommon(PackageMetadata metadata, CommonRepoAccessor commonRepoAccessor) {
        String prefix = metadata.get(PackageMetadata.PREFIX);
        return commonRepoAccessor.makePackageDirectory(prefix);
    }
}
