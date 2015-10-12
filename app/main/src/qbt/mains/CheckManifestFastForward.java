package qbt.mains;

import java.io.IOException;
import java.nio.file.Path;
import misc1.commons.options.OptionsResults;
import qbt.HelpTier;
import qbt.PackageTip;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.QbtManifest;
import qbt.QbtTempDir;
import qbt.RepoManifest;
import qbt.config.QbtConfig;
import qbt.diffmanifests.MapDiffer;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.repo.PinnedRepoAccessor;
import qbt.vcs.LocalVcs;

public class CheckManifestFastForward extends QbtCommand<CheckManifestFastForward.Options> {
    @QbtCommandName("checkManifestFastForward")
    public static interface Options extends QbtCommandOptions {
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final ManifestOptionsDelegate<Options> lhs = new ManifestOptionsDelegate<Options>("lhs");
        public static final ManifestOptionsDelegate<Options> rhs = new ManifestOptionsDelegate<Options>("rhs");
    }

    @Override
    public Class<Options> getOptionsClass() {
        return Options.class;
    }

    @Override
    public HelpTier getHelpTier() {
        return HelpTier.ARCANE;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isProgrammaticOutput() {
        return true;
    }

    @Override
    public int run(OptionsResults<? extends Options> options) throws IOException {
        final QbtConfig config = Options.config.getConfig(options);

        QbtManifest lhs = Options.lhs.getResult(options).parse();
        QbtManifest rhs = Options.rhs.getResult(options).parse();

        new MapDiffer<PackageTip, RepoManifest>(lhs.repos, rhs.repos, PackageTip.COMPARATOR) {
            @Override
            protected void edit(PackageTip repo, RepoManifest lhs, RepoManifest rhs) {
                PinnedRepoAccessor lhsResult = config.localPinsRepo.requirePin(repo, lhs.version);
                PinnedRepoAccessor rhsResult = config.localPinsRepo.requirePin(repo, rhs.version);

                LocalVcs lhsLocalVcs = lhsResult.getLocalVcs();
                LocalVcs rhsLocalVcs = rhsResult.getLocalVcs();
                if(!lhsLocalVcs.equals(rhsLocalVcs)) {
                    throw new RuntimeException("Mismatched local vcs: " + lhsLocalVcs + " / " + rhsLocalVcs);
                }
                LocalVcs localVcs = lhsLocalVcs;

                try(QbtTempDir tempDir = new QbtTempDir()) {
                    Path dir = tempDir.path;
                    localVcs.createWorkingRepo(dir);
                    lhsResult.findCommit(dir);
                    rhsResult.findCommit(dir);

                    if(!localVcs.getRepository(dir).isAncestorOf(lhs.version, rhs.version)) {
                        throw new RuntimeException("Not fast forward: " + repo + ": " + lhs.version.getRawDigest() + " -> " + rhs.version.getRawDigest());
                    }
                }
            }

            @Override
            protected void add(PackageTip repo, RepoManifest manifest) {
            }

            @Override
            protected void del(PackageTip repo, RepoManifest manifest) {
            }
        }.diff();

        return 0;
    }
}
