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
package qbt.artifactcacher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import misc1.commons.ExceptionUtils;
import misc1.commons.Maybe;
import misc1.commons.resources.FreeScope;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import qbt.QbtTempDir;
import qbt.QbtUtils;
import qbt.recursive.cv.CumulativeVersionDigest;
import qbt.utils.TarballUtils;

public class SymlinkingMadnessArtifactCacher implements ArtifactCacher {
    private final Path writeRoot;
    private final Path readRoot;

    public SymlinkingMadnessArtifactCacher(Path root) {
        this(root, root);
    }

    public SymlinkingMadnessArtifactCacher(Path writeRoot, Path readRoot) {
        this.writeRoot = writeRoot;
        this.readRoot = readRoot;
    }

    @Override
    public Pair<Architecture, ArtifactReference> get(FreeScope scope, CumulativeVersionDigest key, Architecture arch) {
        Path dir = readRoot.resolve(key.getRawDigest().toString());
        if(Files.isDirectory(dir)) {
            return Pair.of(Architecture.unknown(), scope.initial(ArtifactReference.TYPE, rawArtifactReference(dir)));
        }
        return null;
    }

    @Override
    public void touch(CumulativeVersionDigest key, Architecture arch) {
    }

    @Override
    public Pair<Architecture, ArtifactReference> intercept(FreeScope scope, CumulativeVersionDigest key, final Pair<Architecture, ArtifactReference> p) {
        final Path writeDir = writeRoot.resolve(key.getRawDigest().toString());
        final Path readDir = readRoot.resolve(key.getRawDigest().toString());

        boolean copied = QbtUtils.semiAtomicDirCache(writeDir, "", (tempDir) -> {
            try {
                Files.delete(tempDir);
            }
            catch(IOException e) {
                throw ExceptionUtils.commute(e);
            }
            p.getRight().materializeDirectory(Maybe.<FreeScope>not(), tempDir);
            return ObjectUtils.NULL;
        });

        Architecture arch;
        if(copied) {
            arch = p.getLeft();
        }
        else {
            // hit on disk, do not honor alleged architecture
            arch = Architecture.unknown();
        }

        return Pair.of(arch, scope.initial(ArtifactReference.TYPE, rawArtifactReference(readDir)));
    }

    private static RawArtifactReference rawArtifactReference(final Path dir) {
        return new RawArtifactReference() {
            @Override
            public void materializeTarball(Path destination) {
                // suxco
                TarballUtils.unexplodeTarball(dir, destination);
            }

            @Override
            public void materializeDirectory(Maybe<FreeScope> scope, Path destination) {
                if(scope.isPresent()) {
                    // when we want to implement cleanup we can add to scope
                    try {
                        Files.createSymbolicLink(destination, dir);
                    }
                    catch(IOException e) {
                        throw ExceptionUtils.commute(e);
                    }
                }
                else {
                    // suxco II
                    try(QbtTempDir temp = new QbtTempDir()) {
                        Path tar = temp.resolve("tar");
                        TarballUtils.unexplodeTarball(dir, tar);
                        try {
                            Files.createDirectory(destination);
                        }
                        catch(IOException e) {
                            throw ExceptionUtils.commute(e);
                        }
                        TarballUtils.explodeTarball(destination, tar);
                    }
                }
            }

            @Override
            public void free() {
                // when we want to implement cleanup we can ref in enclosing
                // and unref here
            }
        };
    }

    @Override
    public void cleanup() {
        // nope, unfortunately
    }
}
