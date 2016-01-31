package qbt.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsException;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.tip.PackageTip;

public class PackageManifestOptionsDelegate<O> implements OptionsDelegate<O> {
    private final OptionsLibrary<O> o = OptionsLibrary.of();
    public final OptionsFragment<O, String> prefix = o.oneArg("prefix").transform(o.singleton(null)).helpDesc("Prefix of package in the repository");
    public final OptionsFragment<O, ImmutableList<String>> normalDependencies = o.oneArg("normalDependency").helpDesc("Normal Dependencies (pkg:type,tip)");
    public final OptionsFragment<O, ImmutableList<String>> verifyDependencies = o.oneArg("verifyDependency").helpDesc("Verify Dependencies (pkg/type)");
    public final OptionsFragment<O, ImmutableList<String>> replaceDependencies = o.oneArg("replaceDependency").helpDesc("Replace Dependencies");
    public final OptionsFragment<O, Boolean> arch = o.zeroArg("archIndependent").transform(o.flag()).helpDesc("Package is architecture independent");
    public final OptionsFragment<O, String> type = o.oneArg("buildType").transform(o.singleton(null)).transform((String h, String s) -> (s != null ? s.toUpperCase() : null)).helpDesc("Package type (normal, copy)");
    public final OptionsFragment<O, ImmutableList<String>> qbtEnv = o.oneArg("qbtEnv").helpDesc("Qbt environment variables");

    public ImmutableSet<Pair<String, Pair<NormalDependencyType, String>>> getNormalDependencies(OptionsResults<? extends O> options) {
        return ImmutableSet.copyOf(Iterables.transform(options.get(normalDependencies), (String s) -> {
            String[] parts = s.split(":");
            if(parts.length == 2) {
                String[] pkg = parts[1].split(",");
                if(pkg.length == 2) {
                    return Pair.of(parts[0], Pair.of(NormalDependencyType.fromTag(pkg[0]), pkg[1]));
                }
                if(pkg.length == 1) {
                    return Pair.of(parts[0], Pair.of(NormalDependencyType.fromTag(pkg[0]), "HEAD"));
                }
            }
            throw new OptionsException("Normal dependencies must be of the form pkg:type[,tip] - could not parse " + s);
        }));
    }

    public ImmutableSet<Pair<PackageTip, String>> getVerifyDependencies(OptionsResults<? extends O> options) {
        return ImmutableSet.copyOf(Iterables.transform(options.get(verifyDependencies), (String s) -> {
            String[] parts = s.split("/");
            if(parts.length == 2) {
                String[] pkg = parts[0].split(",");
                if(pkg.length == 2) {
                    return Pair.of(PackageTip.TYPE.of(pkg[0], pkg[1]), parts[1]);
                }
                if(pkg.length == 1) {
                    return Pair.of(PackageTip.TYPE.of(pkg[0], "HEAD"), parts[1]);
                }
            }
            throw new OptionsException("Verify dependencies must be of the form pkg[,tip]/key - could not parse " + s);
        }));
    }

    public ImmutableSet<Pair<PackageTip, String>> getReplaceDependencies(OptionsResults<? extends O> options) {
        return ImmutableSet.copyOf(Iterables.transform(options.get(replaceDependencies), (String s) -> {
            String[] parts = s.split(":");
            if(parts.length == 2) {
                String[] first = parts[0].split(",");
                PackageTip fpt = null;
                if(first.length == 1) {
                    fpt = PackageTip.TYPE.of(first[0], "HEAD");
                }
                if(first.length == 2) {
                    fpt = PackageTip.TYPE.of(first[0], first[1]);
                }
                if(fpt != null) {
                    return Pair.of(fpt, parts[1]);
                }
            }
            throw new OptionsException("Replace dependencies must be of the form pkg,tip:replacetip - could not parse " + s);
        }));
    }
}
