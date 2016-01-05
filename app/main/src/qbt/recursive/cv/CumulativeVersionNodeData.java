package qbt.recursive.cv;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import misc1.commons.ds.StructKey;
import qbt.VcsTreeDigest;
import qbt.manifest.current.PackageMetadata;

public final class CumulativeVersionNodeData {
    public final String packageName;
    public final VcsTreeDigest effectiveTree;
    public final CumulativeVersionDigest qbtDependency;
    public final PackageMetadata metadata; // stripped!
    public final ImmutableMap<String, String> qbtEnv;

    private static ImmutableMap<String, String> copyEnv(Set<String> keep, Map<String, String> qbtEnv) {
        ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
        List<Map.Entry<String, String>> entries = Lists.newArrayList(qbtEnv.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                int r1 = o1.getKey().compareTo(o2.getKey());
                if(r1 != 0) {
                    return r1;
                }
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        for(Map.Entry<String, String> e : entries) {
            if(!keep.contains(e.getKey())) {
                continue;
            }
            b.put(e);
        }
        return b.build();
    }

    private static boolean includeInCumulativeVersion(StructKey<PackageMetadata, ?, ?> key) {
        if(key == PackageMetadata.PREFIX) {
            return false;
        }
        if(key == PackageMetadata.ARCH_INDEPENDENT) {
            return false;
        }
        if(key == PackageMetadata.QBT_ENV) {
            return false;
        }
        if(key == PackageMetadata.BUILD_TYPE) {
            return true;
        }
        throw new IllegalArgumentException(key.name);
    }

    private static <VS, VB> PackageMetadata.Builder copy(PackageMetadata.Builder b, PackageMetadata metadata, StructKey<PackageMetadata, VS, VB> key) {
        return b.set(key, key.toBuilder(metadata.get(key)));
    }

    private static PackageMetadata stripForCumulativeVersion(PackageMetadata metadata) {
        PackageMetadata.Builder b = PackageMetadata.TYPE.builder();
        for(StructKey<PackageMetadata, ?, ?> key : PackageMetadata.TYPE.keys) {
            if(includeInCumulativeVersion(key)) {
                b = copy(b, metadata, key);
            }
        }
        return b.build();
    }

    public CumulativeVersionNodeData(String packageName, VcsTreeDigest effectiveTree, CumulativeVersionDigest qbtDependency, PackageMetadata metadata, Map<String, String> qbtEnv) {
        this.packageName = packageName;
        this.effectiveTree = effectiveTree;
        this.qbtDependency = qbtDependency;
        this.metadata = stripForCumulativeVersion(metadata);
        this.qbtEnv = copyEnv(metadata.get(PackageMetadata.QBT_ENV), qbtEnv);
    }

    private CumulativeVersionNodeData(Builder b) {
        this.packageName = b.packageName;
        this.effectiveTree = b.effectiveTree;
        this.qbtDependency = b.qbtDependency;
        this.metadata = stripForCumulativeVersion(b.metadata);
        this.qbtEnv = copyEnv(b.metadata.get(PackageMetadata.QBT_ENV), b.qbtEnv);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(packageName, effectiveTree, qbtDependency, metadata, qbtEnv);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof CumulativeVersionNodeData)) {
            return false;
        }
        CumulativeVersionNodeData other = (CumulativeVersionNodeData)obj;
        if(!Objects.equal(packageName, other.packageName)) {
            return false;
        }
        if(!Objects.equal(effectiveTree, other.effectiveTree)) {
            return false;
        }
        if(!Objects.equal(qbtDependency, other.qbtDependency)) {
            return false;
        }
        if(!Objects.equal(metadata, other.metadata)) {
            return false;
        }
        if(!Objects.equal(qbtEnv, other.qbtEnv)) {
            return false;
        }
        return true;
    }

    public static class Builder {
        private String packageName;
        private VcsTreeDigest effectiveTree;
        private CumulativeVersionDigest qbtDependency;
        private PackageMetadata metadata;
        private Map<String, String> qbtEnv;

        private Builder(CumulativeVersionNodeData nodeData) {
            this.packageName = nodeData.packageName;
            this.effectiveTree = nodeData.effectiveTree;
            this.qbtDependency = nodeData.qbtDependency;
            this.metadata = nodeData.metadata;
            this.qbtEnv = nodeData.qbtEnv;
        }

        public CumulativeVersionNodeData build() {
            return new CumulativeVersionNodeData(this);
        }

        public Builder withQbtEnv(Map<String, String> newQbtEnv) {
            qbtEnv = newQbtEnv;
            return this;
        }
    }

    public Builder builder() {
        return new Builder(this);
    }
}
