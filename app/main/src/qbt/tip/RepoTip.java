package qbt.tip;

public final class RepoTip extends AbstractTip<RepoTip> {
    private RepoTip(String name, String tip) {
        super(TYPE, name, tip);
    }

    public PackageTip toPackage(String newName) {
        return PackageTip.TYPE.of(newName, tip);
    }

    public static final Type<RepoTip> TYPE = new Type<RepoTip>(RepoTip.class, "repo") {
        @Override
        public RepoTip of(String name, String tip) {
            return new RepoTip(name, tip);
        }
    };
}
