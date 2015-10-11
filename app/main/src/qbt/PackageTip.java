package qbt;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PackageTip {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^[0-9a-zA-Z._]*$");
    private static final Pattern PACKAGE_TIP_PATTERN = Pattern.compile("^([0-9a-zA-Z._]*)\\^\\{([0-9a-zA-Z._]*)\\}$");

    public final String pkg;
    public final String tip;

    private PackageTip(String pkg, String tip) {
        this.pkg = pkg;
        this.tip = tip;
    }

    @Override
    public int hashCode() {
        return pkg.hashCode() ^ tip.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof PackageTip)) {
            return false;
        }
        PackageTip other = (PackageTip) o;
        if(!pkg.equals(other.pkg)) {
            return false;
        }
        if(!tip.equals(other.tip)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if(tip.equals("HEAD")) {
            return pkg;
        }
        return pkg + "^{" + tip + "}";
    }

    public PackageTip replacePackage(String newPkg) {
        return PackageTip.of(newPkg, tip);
    }

    public PackageTip replaceTip(String newTip) {
        return PackageTip.of(pkg, newTip);
    }

    public static PackageTip of(String pkg, String tip) {
        return new PackageTip(pkg, tip);
    }

    public static PackageTip parseLoose(String arg) {
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(arg);
        if(packageMatcher.matches()) {
            return PackageTip.of(arg, "HEAD");
        }

        Matcher packageTipMatcher = PACKAGE_TIP_PATTERN.matcher(arg);
        if(packageTipMatcher.matches()) {
            return PackageTip.of(packageTipMatcher.group(1), packageTipMatcher.group(2));
        }

        return null;
    }

    public static PackageTip parseRequire(String arg, String type) {
        PackageTip repo = parseLoose(arg);
        if(repo == null) {
            throw new IllegalArgumentException("Neither a " + type + " nor a " + type + " tip: " + arg);
        }
        return repo;
    }

    public static final Comparator<PackageTip> COMPARATOR = new Comparator<PackageTip>() {
        @Override
        public int compare(PackageTip a, PackageTip b) {
            int r;

            r = a.pkg.compareTo(b.pkg);
            if(r != 0) {
                return r;
            }

            r = a.tip.compareTo(b.tip);
            if(r != 0) {
                return r;
            }

            return 0;
        }
    };
}
