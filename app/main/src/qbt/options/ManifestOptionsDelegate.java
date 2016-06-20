//   Copyright 2016 Keith Amling
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
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
import qbt.manifest.LegacyQbtManifest;
import qbt.manifest.QbtManifestVersion;
import qbt.manifest.QbtManifestVersions;
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
            public LegacyQbtManifest<?, ?> parseLegacy() throws IOException {
                return QbtManifestVersions.parseLegacy(getLines());
            }

            @Override
            public QbtManifest parse() throws IOException {
                return QbtManifestVersions.parse(getLines());
            }

            @Override
            public void deparse(QbtManifest manifest) {
                setLines(QbtManifestVersions.toLegacy(manifest).deparse());
            }

            @Override
            public void deparse(LegacyQbtManifest<?, ?> manifest) {
                setLines(manifest.deparse());
            }

            @Override
            public <M, B> ImmutableList<Pair<String, String>> deparseConflict(QbtManifestVersion<M, B> version, String lhsName, M lhs, String mhsName, M mhs, String rhsName, M rhs) {
                Pair<ImmutableList<Pair<String, String>>, ImmutableList<String>> deparse = version.parser().deparse(lhsName, lhs, mhsName, mhs, rhsName, rhs);
                setLines(deparse.getRight());
                return deparse.getLeft();
            }
        };
    }
}
