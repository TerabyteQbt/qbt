package qbt;

import java.nio.file.Path;
import misc1.commons.Maybe;
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
        Maybe<String> prefix = metadata.get(PackageMetadata.PREFIX);
        if(prefix.isPresent()) {
            return commonRepoAccessor.makePackageDirectory(prefix.get(null));
        }
        else {
            final QbtTempDir packageDir = new QbtTempDir();
            return new PackageDirectory() {
                @Override
                public Path getDir() {
                    return packageDir.path;
                }

                @Override
                public void close() {
                    packageDir.close();
                }
            };
        }
    }
}
