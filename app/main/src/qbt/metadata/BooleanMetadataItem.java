package qbt.metadata;

public class BooleanMetadataItem<MT extends MetadataType<MT>> extends MetadataItem<MT, Boolean> {
    public BooleanMetadataItem(String key, boolean valueDefault) {
        super(key, valueDefault);
    }

    @Override
    public String valueToString(Boolean value) {
        return value ? "true" : "false";
    }

    @Override
    public Boolean valueFromString(String value) {
        if(value.equals("true")) {
            return true;
        }
        if(value.equals("false")) {
            return false;
        }
        throw new IllegalArgumentException("Illegal value for boolean " + key + ": " + value);
    }
}
