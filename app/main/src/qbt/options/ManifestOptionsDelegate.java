package qbt.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import misc1.commons.Maybe;
import misc1.commons.options.NamedStringSingletonArgumentOptionsFragment;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsResults;
import org.apache.commons.lang3.tuple.Pair;
import qbt.QbtUtils;
import qbt.manifest.QbtManifest;

public class ManifestOptionsDelegate<O> implements OptionsDelegate<O> {
    public final OptionsFragment<O, ?, String> file;

    public ManifestOptionsDelegate() {
        this("manifest");
    }

    public ManifestOptionsDelegate(String name) {
        this(name, "QBT manifest file");
    }

    public ManifestOptionsDelegate(String name, String helpDesc) {
        this.file = new NamedStringSingletonArgumentOptionsFragment<O>(ImmutableList.of("--" + name), Maybe.<String>of(null), helpDesc);
    }

    private enum Side {
        LHS, MHS, RHS, NONE;
    }

    private static final Map<String, Side> sideMap;
    static {
        ImmutableMap.Builder<String, Side> sideMapBuilder = ImmutableMap.builder();
        for(Side s : Side.values()) {
            sideMapBuilder.put(s.name(), s);
        }
        sideMap = sideMapBuilder.build();
    }

    private static Pair<String, Side> parsePathAndSide(String path) {
        if(path == null) {
            path = "";
        }
        Matcher m = Pattern.compile("^(.*)@\\{([A-Z]*)\\}$").matcher(path);
        Side side = Side.NONE;
        if(m.matches()) {
            Side sideMaybe = sideMap.get(m.group(2));
            if(sideMaybe != null) {
                path = m.group(1);
                side = sideMaybe;
            }
        }
        if(path.equals("")) {
            path = null;
        }
        return Pair.of(path, side);
    }

    public ManifestOptionsResult getResult(OptionsResults<? extends O> options) {
        Pair<String, Side> pair = parsePathAndSide(options.get(file));
        final Path path = QbtUtils.findInMeta("qbt-manifest", pair.getLeft());
        final Side side = pair.getRight();
        return new ManifestOptionsResult() {
            @Override
            public QbtManifest parse() throws IOException {
                switch(side) {
                    case LHS:
                        return QbtManifest.parse(path + "@{LHS}", QbtUtils.parseConflictLines(QbtUtils.readLines(path)).getLeft());

                    case MHS:
                        return QbtManifest.parse(path + "@{MHS}", QbtUtils.parseConflictLines(QbtUtils.readLines(path)).getMiddle());

                    case RHS:
                        return QbtManifest.parse(path + "@{RHS}", QbtUtils.parseConflictLines(QbtUtils.readLines(path)).getRight());

                    case NONE:
                        return QbtManifest.parse(path);
                }
                throw new IllegalStateException(side.name());
            }

            @Override
            public boolean deparseConflict(String lhsName, QbtManifest lhs, String mhsName, QbtManifest mhs, String rhsName, QbtManifest rhs) {
                switch(side) {
                    case LHS:
                    case MHS:
                    case RHS:
                        throw new IllegalArgumentException("Cannot update a side of an existing manifest conflict!");

                    case NONE:
                        Pair<Boolean, Iterable<String>> deparse = QbtManifest.deparseConflicts(lhsName, lhs, mhsName, mhs, rhsName, rhs);
                        QbtUtils.writeLines(path, deparse.getRight());
                        return deparse.getLeft();
                }
                throw new IllegalStateException(side.name());
            }

            @Override
            public void deparse(QbtManifest manifest) {
                switch(side) {
                    case LHS:
                    case MHS:
                    case RHS:
                        throw new IllegalArgumentException("Cannot update a side of an existing manifest conflict!");

                    case NONE:
                        QbtUtils.writeLines(path, manifest.deparse());
                        return;
                }
                throw new IllegalStateException(side.name());
            }
        };
    }
}
