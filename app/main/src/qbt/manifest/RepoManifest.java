package qbt.manifest;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import qbt.VcsVersionDigest;

public final class RepoManifest {
    public final VcsVersionDigest version;
    public final Map<String, PackageManifest> packages;

    private RepoManifest(VcsVersionDigest version, ImmutableMap<String, PackageManifest> packages) {
        this.version = version;
        this.packages = packages;
    }

    private RepoManifest(Builder b) {
        this.version = b.version;
        this.packages = b.packages.build();
    }

    public static final class Builder {
        private VcsVersionDigest version;
        private final ImmutableMap.Builder<String, PackageManifest> packages = ImmutableMap.builder();

        private Builder(VcsVersionDigest version) {
            this.version = version;
        }

        private Builder(RepoManifest manifest) {
            this(manifest.version);
            packages.putAll(manifest.packages);
        }

        public Builder withVersion(VcsVersionDigest version) {
            this.version = version;
            return this;
        }

        public Builder with(String pkg, PackageManifest manifest) {
            packages.put(pkg, manifest);
            return this;
        }

        public RepoManifest build() {
            return new RepoManifest(this);
        }
    }

    public static Builder builder(VcsVersionDigest version) {
        return new Builder(version);
    }

    public Builder builder() {
        return new Builder(this);
    }

    @Override
    public int hashCode() {
        int r = 0;
        r = 31 * r + version.hashCode();
        r = 31 * r + packages.hashCode();
        return r;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof RepoManifest)) {
            return false;
        }
        RepoManifest other = (RepoManifest) obj;
        if(!version.equals(other.version)) {
            return false;
        }
        if(!packages.equals(other.packages)) {
            return false;
        }
        return true;
    }

    public static RepoManifest of(VcsVersionDigest version, Map<String, PackageManifest> packages) {
        return new RepoManifest(version, ImmutableMap.copyOf(packages));
    }
}
