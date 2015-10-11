package qbt.metadata;

public class EnumMetadataItem<MT extends MetadataType<MT>, T extends Enum<T>> extends MetadataItem<MT, T> {
    private final Class<T> clazz;

    public EnumMetadataItem(String key, Class<T> clazz, T valueDefault) {
        super(key, valueDefault);
        this.clazz = clazz;
    }

    @Override
    public String valueToString(T value) {
        return value.toString();
    }

    @Override
    public T valueFromString(String value) {
        return Enum.valueOf(clazz, value);
    }
}
