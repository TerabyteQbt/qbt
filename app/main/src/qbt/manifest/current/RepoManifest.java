package qbt.manifest.current;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructBuilder;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import misc1.commons.ds.StructTypeBuilder;
import qbt.VcsVersionDigest;
import qbt.manifest.QbtJsonSerializers;

public final class RepoManifest extends Struct<RepoManifest, RepoManifest.Builder> {
    public final Optional<VcsVersionDigest> version;
    public final ImmutableMap<String, PackageManifest> packages;

    private RepoManifest(ImmutableMap<StructKey<RepoManifest, ?, ?>, Object> map) {
        super(TYPE, map);

        this.version = get(VERSION);
        this.packages = get(PACKAGES).map;
    }

    public static class Builder extends StructBuilder<RepoManifest, Builder> {
        public Builder(ImmutableSalvagingMap<StructKey<RepoManifest, ?, ?>, Object> map) {
            super(TYPE, map);
        }
    }

    public static final StructKey<RepoManifest, RepoManifestPackages, RepoManifestPackages.Builder> PACKAGES;
    public static final StructKey<RepoManifest, Optional<VcsVersionDigest>, Optional<VcsVersionDigest>> VERSION;
    public static final StructType<RepoManifest, Builder> TYPE;
    static {
        StructTypeBuilder<RepoManifest, Builder> b = new StructTypeBuilder<>(RepoManifest::new, Builder::new);

        PACKAGES = b.key("packages", RepoManifestPackages.TYPE).add();
        VERSION = b.<Optional<VcsVersionDigest>>key("version").serializer(QbtJsonSerializers.OPTIONAL_VCS_VERSION_DIGEST).add();

        TYPE = b.build();
    }
}
