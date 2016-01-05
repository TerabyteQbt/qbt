package qbt.recursive.cv;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Hasher;
import com.google.gson.JsonElement;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.QbtHashUtils;
import qbt.VcsTreeDigest;
import qbt.manifest.current.PackageMetadata;
import qbt.recursive.rpd.RecursivePackageData;
import qbt.recursive.rpd.RecursivePackageDataCanonicalizer;

public final class CumulativeVersion extends RecursivePackageData<CumulativeVersionNodeData, CumulativeVersion> {
    private final CumulativeVersionDigest digest;
    private final Map<String, Pair<NormalDependencyType, CumulativeVersion>> normalDependenciesSorted;

    private static final String INDENT = "    ";

    private CumulativeVersion(CumulativeVersionNodeData nodeData, Map<String, Pair<NormalDependencyType, CumulativeVersion>> normalDependencies) {
        super(nodeData, normalDependencies);

        Hasher b = QbtHashUtils.newHasher();

        b = b.putString("SELF\t", Charsets.UTF_8);
        b = b.putString(nodeData.packageName, Charsets.UTF_8);
        b = b.putString("\t", Charsets.UTF_8);
        b = b.putString(nodeData.effectiveTree.getRawDigest().toString(), Charsets.UTF_8);
        b = b.putString("\n", Charsets.UTF_8);
        b = b.putString("QBT\t", Charsets.UTF_8);
        b = b.putString(nodeData.qbtDependency.getRawDigest().toString(), Charsets.UTF_8);
        for(Map.Entry<String, String> e : nodeData.qbtEnv.entrySet()) {
            b = b.putString("QBTENV\t", Charsets.UTF_8);
            b = b.putString(QbtHashUtils.hashFunction().hashString(e.getKey(), Charsets.UTF_8).toString(), Charsets.UTF_8);
            b = b.putString("\t", Charsets.UTF_8);
            b = b.putString(QbtHashUtils.hashFunction().hashString(e.getValue(), Charsets.UTF_8).toString(), Charsets.UTF_8);
            b = b.putString("\n", Charsets.UTF_8);
        }
        for(Map.Entry<String, String> e : toStringMap(nodeData.metadata)) {
            b = b.putString("METADATA\t", Charsets.UTF_8);
            b = b.putString(e.getKey(), Charsets.UTF_8);
            b = b.putString("\t", Charsets.UTF_8);
            b = b.putString(e.getValue(), Charsets.UTF_8);
            b = b.putString("\n", Charsets.UTF_8);
        }
        List<Map.Entry<String, Pair<NormalDependencyType, CumulativeVersion>>> normalDependenciesEntriesSorted = Lists.newArrayList(normalDependencies.entrySet());
        Collections.sort(normalDependenciesEntriesSorted, new Comparator<Map.Entry<String, Pair<NormalDependencyType, CumulativeVersion>>>() {
            @Override
            public int compare(Map.Entry<String, Pair<NormalDependencyType, CumulativeVersion>> o1, Map.Entry<String, Pair<NormalDependencyType, CumulativeVersion>> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        ImmutableMap.Builder<String, Pair<NormalDependencyType, CumulativeVersion>> normalDependenciesSortedBuilder = ImmutableMap.builder();
        for(Map.Entry<String, Pair<NormalDependencyType, CumulativeVersion>> e : normalDependenciesEntriesSorted) {
            normalDependenciesSortedBuilder.put(e);
        }
        this.normalDependenciesSorted = normalDependenciesSortedBuilder.build();
        for(Map.Entry<String, Pair<NormalDependencyType, CumulativeVersion>> e : normalDependenciesSorted.entrySet()) {
            b = b.putString(e.getValue().getLeft().getTag(), Charsets.UTF_8);
            b = b.putString("\t", Charsets.UTF_8);
            b = b.putString(e.getKey(), Charsets.UTF_8);
            b = b.putString("\t", Charsets.UTF_8);
            b = b.putString(e.getValue().getRight().getDigest().getRawDigest().toString(), Charsets.UTF_8);
            b = b.putString("\n", Charsets.UTF_8);
        }

        this.digest = new CumulativeVersionDigest(b.hash());
    }

    private static final RecursivePackageDataCanonicalizer<CumulativeVersionNodeData, CumulativeVersion, CumulativeVersionNodeData> canonicalizer = new RecursivePackageDataCanonicalizer<CumulativeVersionNodeData, CumulativeVersion, CumulativeVersionNodeData>() {
        @Override
        protected CumulativeVersionNodeData key(CumulativeVersion r) {
            return r.result;
        }

        @Override
        protected CumulativeVersion newR(CumulativeVersionNodeData nodeData, Map<String, Pair<NormalDependencyType, CumulativeVersion>> dependencyVersions) {
            return new CumulativeVersion(nodeData, dependencyVersions);
        }
    };

    public static CumulativeVersion of(CumulativeVersionNodeData nodeData, Map<String, Pair<NormalDependencyType, CumulativeVersion>> normalDependencies) {
        return canonicalizer.transform(new CumulativeVersion(nodeData, ImmutableMap.copyOf(normalDependencies)));
    }

    public String getPackageName() {
        return result.packageName;
    }

    public VcsTreeDigest getEffectiveTree() {
        return result.effectiveTree;
    }

    public CumulativeVersionDigest getQbtDependency() {
        return result.qbtDependency;
    }

    public PackageMetadata getMetadata() {
        return result.metadata;
    }

    public Map<String, Pair<NormalDependencyType, CumulativeVersion>> getNormalDependencies() {
        return normalDependenciesSorted;
    }

    public CumulativeVersionDigest getDigest() {
        return digest;
    }

    @Override
    public String toString() {
        return "CumulativeVersion{" + getPackageName() + "@" + digest.getRawDigest() + "}";
    }

    public String prettyDigest() {
        return getPackageName() + "@" + digest.getRawDigest();
    }

    public List<String> prettyTree() {
        ImmutableList.Builder<String> b = ImmutableList.builder();
        prettyTree("", b, Sets.<CumulativeVersion>newHashSet());
        return b.build();
    }

    private void prettyTree(String prefix, ImmutableList.Builder<String> b, Set<CumulativeVersion> seen) {
        if(!seen.add(this)) {
            b.add(prefix + prettyDigest() + "...");
            return;
        }

        b.add(prefix + prettyDigest() + ":");
        b.add(prefix + INDENT + "EffectiveTree: " + getEffectiveTree().getRawDigest());
        b.add(prefix + INDENT + "QbtVersion: " + getQbtDependency().getRawDigest());

        Map<String, String> qbtEnv = result.qbtEnv;
        if(!qbtEnv.isEmpty()) {
            b.add(prefix + INDENT + "QbtEnv:");
            for(Map.Entry<String, String> e : qbtEnv.entrySet()) {
                b.add(prefix + INDENT + INDENT + e.getKey() + ": " + e.getValue());
            }
        }

        Collection<Map.Entry<String, String>> metadataMap = toStringMap(getMetadata());
        if(!metadataMap.isEmpty()) {
            b.add(prefix + INDENT + "Metadata:");
            for(Map.Entry<String, String> e : metadataMap) {
                b.add(prefix + INDENT + INDENT + e.getKey() + ": " + e.getValue());
            }
        }

        for(NormalDependencyType type : NormalDependencyType.values()) {
            boolean first = true;
            for(Pair<NormalDependencyType, CumulativeVersion> e : getNormalDependencies().values()) {
                if(type != e.getLeft()) {
                    continue;
                }
                if(first) {
                    b.add(prefix + INDENT + type.getTag() + ":");
                    first = false;
                }
                e.getRight().prettyTree(prefix + INDENT + INDENT, b, seen);
            }
        }
    }

    private static Collection<Map.Entry<String, String>> toStringMap(PackageMetadata metadata) {
        JsonElement e = PackageMetadata.SERIALIZER.toJson(metadata.builder());
        TreeMap<String, String> m = Maps.newTreeMap();
        for(Map.Entry<String, JsonElement> e2 : e.getAsJsonObject().entrySet()) {
            m.put(e2.getKey(), e2.getValue().getAsString());
        }
        return m.entrySet();
    }
}
