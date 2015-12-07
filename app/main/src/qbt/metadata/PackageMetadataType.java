package qbt.metadata;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import misc1.commons.Maybe;

public enum PackageMetadataType implements MetadataType<PackageMetadataType> {
    INSTANCE;

    private static final ImmutableMap<String, MetadataItem<PackageMetadataType, ?>> ITEMS;
    private static final ImmutableSet<MetadataItem<PackageMetadataType, ?>> ITEMS_IN_CV;
    public static final MetadataItem<PackageMetadataType, Maybe<String>> PREFIX;
    public static final MetadataItem<PackageMetadataType, Boolean> ARCH_INDEPENDENT;
    public static final MetadataItem<PackageMetadataType, Set<String>> QBT_ENV;
    public static final MetadataItem<PackageMetadataType, PackageBuildType> BUILD_TYPE;
    static {
        class ItemsBuilder {
            ImmutableMap.Builder<String, MetadataItem<PackageMetadataType, ?>> items = ImmutableMap.builder();
            ImmutableSet.Builder<MetadataItem<PackageMetadataType, ?>> itemsInCv = ImmutableSet.builder();

            public void put(boolean includeInCumulativeVersion, MetadataItem<PackageMetadataType, ?> item) {
                items.put(item.key, item);
                if(includeInCumulativeVersion) {
                    itemsInCv.add(item);
                }
            }
        }
        ItemsBuilder b = new ItemsBuilder();

        b.put(false, PREFIX = new MetadataItem<PackageMetadataType, Maybe<String>>("prefix", Maybe.of("")) {
            @Override
            public String valueToString(Maybe<String> value) {
                return value.get("NONE");
            }

            @Override
            public Maybe<String> valueFromString(String value) {
                if(value.equals("NONE")) {
                    return Maybe.not();
                }
                return Maybe.of(value);
            }
        });
        b.put(false, ARCH_INDEPENDENT = new BooleanMetadataItem<PackageMetadataType>("archIndependent", false));
        b.put(false, QBT_ENV = new MetadataItem<PackageMetadataType, Set<String>>("qbtEnv", ImmutableSet.<String>of()) {
            @Override
            public String valueToString(Set<String> value) {
                List<String> valueOrdered = Lists.newArrayList(value);
                Collections.sort(valueOrdered);
                return Joiner.on(",").join(valueOrdered);
            }

            @Override
            public Set<String> valueFromString(String value) {
                List<String> list = Lists.newArrayList(value.split(","));
                Collections.sort(list);
                return ImmutableSet.copyOf(list);
            }
        });
        b.put(true, BUILD_TYPE = new EnumMetadataItem<PackageMetadataType, PackageBuildType>("buildType", PackageBuildType.class, PackageBuildType.NORMAL));

        ITEMS = b.items.build();
        ITEMS_IN_CV = b.itemsInCv.build();
    }

    @Override
    public MetadataItem<PackageMetadataType, ?> itemFromString(String item) {
        MetadataItem<PackageMetadataType, ?> ret = ITEMS.get(item);
        if(ret == null) {
            throw new IllegalArgumentException("No such package metadata item: " + item);
        }
        return ret;
    }

    public static Metadata<PackageMetadataType> fromStringMap(Map<String, String> right) {
        return Metadata.fromStringMap(INSTANCE, right);
    }

    public static Metadata<PackageMetadataType> of() {
        return Metadata.of(INSTANCE);
    }

    public static Metadata<PackageMetadataType> stripForCumulativeVersion(Metadata<PackageMetadataType> metadata) {
        return metadata.pruneTo(new Metadata.EntryPredicate<PackageMetadataType>() {
            @Override
            public <T> boolean apply(MetadataItem<PackageMetadataType, T> item, T value) {
                return ITEMS_IN_CV.contains(item);
            }
        });
    }
}
