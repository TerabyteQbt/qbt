package qbt;

public enum HelpTier {
    COMMON(true),
    UNCOMMON(true),
    ARCANE(false),
    PLUMBING(false);

    public final boolean showByDefault;

    private HelpTier(boolean showByDefault) {
        this.showByDefault = showByDefault;
    }
}
