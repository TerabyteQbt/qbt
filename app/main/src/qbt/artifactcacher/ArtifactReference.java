package qbt.artifactcacher;

import com.google.common.base.Function;
import java.nio.file.Path;
import misc1.commons.Maybe;
import misc1.commons.resources.FreeScope;
import misc1.commons.resources.Resource;
import misc1.commons.resources.ResourceType;

public interface ArtifactReference extends BaseArtifactReference, Resource<ArtifactReference> {
    public static final ResourceType<RawArtifactReference, ArtifactReference> TYPE = new ResourceType<RawArtifactReference, ArtifactReference>() {
        public ArtifactReference wrap(final RawArtifactReference raw, final Function<FreeScope, ArtifactReference> onCopyInto) {
            return new ArtifactReference() {
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
    };
}
