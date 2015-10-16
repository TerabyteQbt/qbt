package qbt;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import qbt.metadata.Metadata;
import qbt.metadata.MetadataItem;
import qbt.metadata.PackageMetadataType;
import qbt.tip.PackageTip;

public final class PackageManifest {
    public final Metadata<PackageMetadataType> metadata;
    public final Map<String, Pair<NormalDependencyType, String>> normalDeps;
    public final Map<PackageTip, String> replaceDeps;

    private PackageManifest(Metadata<PackageMetadataType> metadata, Map<String, Pair<NormalDependencyType, String>> normalDeps, Map<PackageTip, String> replaceDeps) {
        this.metadata = metadata;
        this.normalDeps = normalDeps;
        this.replaceDeps = replaceDeps;
    }

    private PackageManifest(Builder b) {
        this.metadata = b.metadata.build();
        this.normalDeps = b.normalDeps.build();
        this.replaceDeps = b.replaceDeps.build();
    }

    public static class Builder {
        private final Metadata.Builder<PackageMetadataType> metadata;
        private final ImmutableMap.Builder<String, Pair<NormalDependencyType, String>> normalDeps = ImmutableMap.builder();
        private final ImmutableMap.Builder<PackageTip, String> replaceDeps = ImmutableMap.builder();

        private Builder(Metadata<PackageMetadataType> metadata) {
            this.metadata = metadata.builder();
        }

        public <T> Builder withMetadata(MetadataItem<PackageMetadataType, T> item, T value) {
            metadata.put(item, value);
            return this;
        }

        public Builder withStringMetadata(String item, String value) {
            metadata.putString(item, value);
            return this;
        }

        public Builder withNormalDep(PackageTip p, NormalDependencyType type) {
            normalDeps.put(p.name, Pair.of(type, p.tip));
            return this;
        }

        public Builder withReplaceDep(PackageTip p, String s) {
            replaceDeps.put(p, s);
            return this;
        }

        public PackageManifest build() {
            return new PackageManifest(this);
        }
    }

    public static Builder emptyBuilder() {
        return new Builder(PackageMetadataType.of());
    }

    // discouraged due to high coupling with exact contents
    public static PackageManifest of(Metadata<PackageMetadataType> metadata, Map<String, Pair<NormalDependencyType, String>> normalDeps, Map<PackageTip, String> replaceDeps) {
        return new PackageManifest(metadata, normalDeps, replaceDeps);
    }

    @Override
    public int hashCode() {
        int r = 0;
        r = 31 * r + metadata.hashCode();
        r = 31 * r + normalDeps.hashCode();
        r = 31 * r + replaceDeps.hashCode();
        return r;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof PackageManifest)) {
            return false;
        }
        PackageManifest other = (PackageManifest) obj;
        if(!metadata.equals(other.metadata)) {
            return false;
        }
        if(!normalDeps.equals(other.normalDeps)) {
            return false;
        }
        if(!replaceDeps.equals(other.replaceDeps)) {
            return false;
        }
        return true;
    }
}
