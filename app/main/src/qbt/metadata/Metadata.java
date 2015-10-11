package qbt.metadata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Metadata<MT extends MetadataType<MT>> {
    private final MT metadataType;
    private final ImmutableMap<MetadataItem<MT, ?>, Object> values;

    protected Metadata(MT metadataType, ImmutableMap<MetadataItem<MT, ?>, Object> values) {
        this.metadataType = metadataType;
        this.values = values;
    }

    public static final class Builder<MT extends MetadataType<MT>> {
        private final MT metadataType;
        private Map<MetadataItem<MT, ?>, Object> values;

        private Builder(Metadata<MT> metadata) {
            this.metadataType = metadata.metadataType;
            // this sucks, but I have to be able to overwrite... damn you ImmutableMap.Builder
            this.values = Maps.newHashMap(metadata.values);
        }

        public <T> Builder<MT> put(MetadataItem<MT, ?> item, T value) {
            if(item.valueDefault.equals(value)) {
                values.remove(item);
            }
            else {
                values.put(item, value);
            }
            return this;
        }

        public <T> Builder<MT> putString(MetadataItem<MT, ?> item, String value) {
            return put(item, item.valueFromString(value));
        }

        public Builder<MT> putString(String item, String value) {
            return putString(metadataType.itemFromString(item), value);
        }

        public Metadata<MT> build() {
            return new Metadata<MT>(metadataType, ImmutableMap.copyOf(values));
        }
    }

    public Builder<MT> builder() {
        return new Builder<MT>(this);
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
