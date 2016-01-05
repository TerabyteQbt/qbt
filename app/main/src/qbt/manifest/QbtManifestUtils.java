package qbt.manifest;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import qbt.manifest.current.RepoManifest;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public final class QbtManifestUtils {
    private QbtManifestUtils() {
        // nope
    }

    public static ImmutableMap<PackageTip, RepoTip> invertReposMap(ImmutableMap<RepoTip, RepoManifest> repos) {
        ImmutableMap.Builder<PackageTip, RepoTip> b = ImmutableMap.builder();
        for(Map.Entry<RepoTip, RepoManifest> e : repos.entrySet()) {
            for(String pkg : e.getValue().packages.keySet()) {
                b.put(e.getKey().toPackage(pkg), e.getKey());
            }
        }
        return b.build();
    }
}
