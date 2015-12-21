package qbt.metadata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import misc1.commons.ds.ImmutableSalvagingMap;
import misc1.commons.merge.Merge;
import org.apache.commons.lang3.tuple.Triple;

public class Metadata<MT extends MetadataType<MT>> {
    private final MT metadataType;
    private final ImmutableMap<MetadataItem<MT, ?>, Object> values;

    protected Metadata(MT metadataType, ImmutableMap<MetadataItem<MT, ?>, Object> values) {
        this.metadataType = metadataType;
        this.values = values;
    }

    public static final class Builder<MT extends MetadataType<MT>> {
        private final MT metadataType;
        private final ImmutableSalvagingMap<MetadataItem<MT, ?>, Object> values;

        private Builder(MT metadataType, ImmutableSalvagingMap<MetadataItem<MT, ?>, Object> values) {
            this.metadataType = metadataType;
            this.values = values;
        }

        public <T> Builder<MT> put(MetadataItem<MT, ?> item, T value) {
            ImmutableSalvagingMap<MetadataItem<MT, ?>, Object> valuesNew;
            if(item.valueDefault.equals(value)) {
                valuesNew = values.simpleRemove(item);
            }
            else {
                valuesNew = values.simplePut(item, value);
            }
            return new Builder(metadataType, valuesNew);
        }

        public <T> Builder<MT> putString(MetadataItem<MT, ?> item, String value) {
            return put(item, item.valueFromString(value));
        }

        public Builder<MT> putString(String item, String value) {
            return putString(metadataType.itemFromString(item), value);
        }

        public Metadata<MT> build() {
            return new Metadata<MT>(metadataType, values.toMap());
        }
    }

    public Builder<MT> builder() {
        return new Builder<MT>(metadataType, ImmutableSalvagingMap.copyOf(values));
    }

    public static <MT extends MetadataType<MT>> Metadata<MT> of(MT metadataType) {
        return new Metadata<MT>(metadataType, ImmutableMap.<MetadataItem<MT, ?>, Object>of());
    }

    public <T> T get(MetadataItem<MT, T> item) {
        @SuppressWarnings("unchecked")
        T value = (T)values.get(item);
        if(value == null) {
            value = item.valueDefault;
        }
        return value;
    }

    private <T> String toStringItem(MetadataItem<MT, T> item) {
        return item.valueToString(get(item));
    }

    public Map<String, String> toStringMap() {
        ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
        List<MetadataItem<MT, ?>> items = Lists.newArrayList(values.keySet());
        Collections.sort(items, new Comparator<MetadataItem<MT, ?>>() {
            @Override
            public int compare(MetadataItem<MT, ?> o1, MetadataItem<MT, ?> o2) {
                return o1.key.compareTo(o2.key);
            }
        });
        for(MetadataItem<MT, ?> item : items) {
            b.put(item.key, toStringItem(item));
        }
        return b.build();
    }

    public interface EntryPredicate<MT> {
        <T> boolean apply(MetadataItem<MT, T> item, T value);
    }

    public Metadata<MT> pruneTo(EntryPredicate<MT> predicate) {
        Builder<MT> b = of(metadataType).builder();
        for(MetadataItem<MT, ?> item : values.keySet()) {
            maybeCopyItem(b, item, predicate);
        }
        return b.build();
    }

    private <T> void maybeCopyItem(Builder<MT> b, MetadataItem<MT, T> item, EntryPredicate<MT> predicate) {
        T value = get(item);
        if(predicate.apply(item, value)) {
            b.put(item, value);
        }
    }

    public static <MT extends MetadataType<MT>> Metadata<MT> fromStringMap(MT metadataType, Map<String, String> strings) {
        Builder<MT> b = of(metadataType).builder();
        for(Map.Entry<String, String> e : strings.entrySet()) {
            b = b.putString(e.getKey(), e.getValue());
        }
        return b.build();
    }

    public static <MT extends MetadataType<MT>> Merge<Metadata<MT>> merge(final MT metadataType) {
        return new Merge<Metadata<MT>>() {
            @Override
            public Triple<Metadata<MT>, Metadata<MT>, Metadata<MT>> merge(final Metadata<MT> lhs, final Metadata<MT> mhs, final Metadata<MT> rhs) {
                ImmutableSet.Builder<MetadataItem<MT, ?>> items = ImmutableSet.builder();
                items.addAll(lhs.values.keySet());
                items.addAll(mhs.values.keySet());
                items.addAll(rhs.values.keySet());

                class Helper {
                    private Metadata.Builder<MT> lhsB = of(metadataType).builder();
                    private Metadata.Builder<MT> mhsB = of(metadataType).builder();
                    private Metadata.Builder<MT> rhsB = of(metadataType).builder();

                    private <T> void mergeItem(MetadataItem<MT, T> item) {
                        Triple<T, T, T> r = item.merge().merge(lhs.get(item), mhs.get(item), rhs.get(item));
                        lhsB = lhsB.put(item, r.getLeft());
                        mhsB = lhsB.put(item, r.getMiddle());
                        rhsB = lhsB.put(item, r.getRight());
                    }
                }
                Helper h = new Helper();

                for(MetadataItem<MT, ?> item : items.build()) {
                    h.mergeItem(item);
                }

                return Triple.of(h.lhsB.build(), h.mhsB.build(), h.rhsB.build());
            }
        };
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Metadata)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Metadata<MT> other = (Metadata<MT>)obj;
        return values.equals(other.values);
    }
}
