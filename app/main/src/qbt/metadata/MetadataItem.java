package qbt.metadata;

import misc1.commons.merge.Merge;
import misc1.commons.merge.Merges;

public abstract class MetadataItem<MT, T> {
    public final String key;
    public final T valueDefault;

    public MetadataItem(String key, T valueDefault) {
        this.key = key;
        this.valueDefault = valueDefault;
    }

    public abstract T valueFromString(String value);
    public abstract String valueToString(T value);

    public Merge<T> merge() {
        return Merges.trivial();
    }
}
