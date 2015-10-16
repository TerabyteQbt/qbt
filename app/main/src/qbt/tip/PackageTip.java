package qbt.tip;

public final class PackageTip extends AbstractTip<PackageTip> {
    private PackageTip(String name, String tip) {
        super(TYPE, name, tip);
    }

    public static final Type<PackageTip> TYPE = new Type<PackageTip>("package") {
        @Override
        public PackageTip of(String name, String tip) {
            return new PackageTip(name, tip);
        }
    };
}
