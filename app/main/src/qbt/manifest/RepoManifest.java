package qbt.manifest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.ds.SimpleStructKey;
import misc1.commons.ds.Struct;
import misc1.commons.ds.StructBuilder;
import misc1.commons.ds.StructKey;
import misc1.commons.ds.StructType;
import misc1.commons.merge.Merge;
import qbt.VcsVersionDigest;

public final class RepoManifest extends Struct<RepoManifest, RepoManifest.Builder> {
    public final VcsVersionDigest version;
    public final Map<String, PackageManifest> packages;

    private RepoManifest(ImmutableMap<StructKey<RepoManifest, ?, ?>, Object> map) {
        super(TYPE, map);
        this.version = get(VERSION);
        this.packages = get(PACKAGES).packages;
    }

    public static class Builder extends StructBuilder<RepoManifest, Builder> {
        public Builder(ImmutableSalvagingMap<StructKey<RepoManifest, ?, ?>, Object> map) {
            super(TYPE, map);
        }

        public Builder with(String pkg, PackageManifest.Builder packageBuilder) {
            RepoManifestPackages.Builder packagesBuilder = get(PACKAGES);
            packagesBuilder = packagesBuilder.with(pkg, packageBuilder);
            return set(PACKAGES, packagesBuilder);
        }
    }

    public static final SimpleStructKey<RepoManifest, VcsVersionDigest> VERSION;
    public static final StructKey<RepoManifest, RepoManifestPackages, RepoManifestPackages.Builder> PACKAGES;
    public static final StructType<RepoManifest, Builder> TYPE;
    static {
        ImmutableList.Builder<StructKey<RepoManifest, ?, ?>> b = ImmutableList.builder();

        b.add(VERSION = new SimpleStructKey<RepoManifest, VcsVersionDigest>("version"));
        b.add(PACKAGES = new StructKey<RepoManifest, RepoManifestPackages, RepoManifestPackages.Builder>("packages", RepoManifestPackages.TYPE.builder()) {
            @Override
            public RepoManifestPackages toStruct(RepoManifestPackages.Builder vb) {
                return vb.build();
            }

            @Override
            public RepoManifestPackages.Builder toBuilder(RepoManifestPackages vs) {
                return vs.builder();
            }

            @Override
            public Merge<RepoManifestPackages> merge() {
                return RepoManifestPackages.TYPE.merge();
            }
        });

        TYPE = new StructType<RepoManifest, Builder>(b.build()) {
            @Override
            protected RepoManifest createUnchecked(ImmutableMap<StructKey<RepoManifest, ?, ?>, Object> map) {
                return new RepoManifest(map);
            }

            @Override
            protected RepoManifest.Builder createBuilder(ImmutableSalvagingMap<StructKey<RepoManifest, ?, ?>, Object> map) {
                return new Builder(map);
            }
        };
    }
}
