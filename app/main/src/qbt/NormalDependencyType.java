package qbt;

public enum NormalDependencyType {
    STRONG("Strong", "S"),
    RUNTIME_WEAK("RuntimeWeak"),
    BUILDTIME_WEAK("Weak", "W"),
    ;

    private final String tag;
    private final String[] legacyTags;

    private NormalDependencyType(String tag, String... legacyTags) {
        this.tag = tag;
        this.legacyTags = legacyTags;
    }

    public String getTag() {
        return tag;
    }

    public static NormalDependencyType fromTag(String tag) {
        for(NormalDependencyType v : values()) {
            if(v.tag.equals(tag)) {
                return v;
            }
            for(String legacyTag : v.legacyTags) {
                if(legacyTag.equals(tag)) {
                    return v;
                }
            }
        }
        throw new IllegalArgumentException("No NormalDependencyType has tag: " + tag);
    }
}
