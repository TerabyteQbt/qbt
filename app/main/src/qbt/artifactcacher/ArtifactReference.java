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

import java.nio.file.Path;
import misc1.commons.Maybe;
import misc1.commons.resources.FreeScope;
import misc1.commons.resources.Resource;
import misc1.commons.resources.ResourceType;

public interface ArtifactReference extends BaseArtifactReference, Resource<ArtifactReference> {
    public static final ResourceType<RawArtifactReference, ArtifactReference> TYPE = (raw, onCopyInto) -> new ArtifactReference() {
        @Override
        public void materializeDirectory(Maybe<FreeScope> scope, Path destination) {
            raw.materializeDirectory(scope, destination);
        }

        @Override
        public void materializeTarball(Path destination) {
            raw.materializeTarball(destination);
        }

        public ArtifactReference copyInto(FreeScope scope) {
            return onCopyInto.apply(scope);
        }
    };
}
