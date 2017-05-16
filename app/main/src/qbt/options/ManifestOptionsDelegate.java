package qbt.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import misc1.commons.options.OptionsDelegate;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;
import org.apache.commons.lang3.tuple.Pair;
import qbt.QbtUtils;
import qbt.manifest.QbtManifestParser;
import qbt.manifest.current.QbtManifest;

public class ManifestOptionsDelegate<O> implements OptionsDelegate<O> {
    public final OptionsFragment<O, String> file;

    public ManifestOptionsDelegate() {
        this("manifest");
    }

    public ManifestOptionsDelegate(String name) {
        this(name, "QBT manifest file");
    }

    public ManifestOptionsDelegate(String name, String helpDesc) {
        OptionsLibrary<O> o = OptionsLibrary.of();
        this.file = o.oneArg(name).transform(o.singleton(null)).helpDesc(helpDesc);
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
            private ImmutableList<String> getLines() {
                switch(side) {
                    case LHS:
                        return QbtUtils.parseConflictLines(QbtUtils.readLines(path)).getLeft();

                    case MHS:
                        return QbtUtils.parseConflictLines(QbtUtils.readLines(path)).getMiddle();

                    case RHS:
                        return QbtUtils.parseConflictLines(QbtUtils.readLines(path)).getRight();

                    case NONE:
                        return QbtUtils.readLines(path);
                }
                throw new IllegalStateException(side.name());
            }

            private void setLines(ImmutableList<String> lines) {
                switch(side) {
                    case LHS:
                    case MHS:
                    case RHS:
                        throw new IllegalArgumentException("Cannot update a side of an existing manifest conflict!");

                    case NONE:
                        QbtUtils.writeLines(path, lines);
                        return;
                }
                throw new IllegalStateException(side.name());
            }

            @Override
            public QbtManifest parse(QbtManifestParser parser) throws IOException {
                return parser.parse(getLines());
            }

            @Override
            public void deparse(QbtManifestParser parser, QbtManifest manifest) {
                setLines(parser.deparse(manifest));
            }

            @Override
            public ImmutableList<Pair<String, String>> deparseConflict(QbtManifestParser parser, String lhsName, QbtManifest lhs, String mhsName, QbtManifest mhs, String rhsName, QbtManifest rhs) {
                Pair<ImmutableList<Pair<String, String>>, ImmutableList<String>> deparse = parser.deparse(lhsName, lhs, mhsName, mhs, rhsName, rhs);
                setLines(deparse.getRight());
                return deparse.getLeft();
            }
        };
    }
}
