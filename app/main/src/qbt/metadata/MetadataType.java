package qbt.metadata;

public interface MetadataType<MT> {
    MetadataItem<MT, ?> itemFromString(String item);
}
