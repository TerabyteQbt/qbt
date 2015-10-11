package qbt.mains;

import com.google.common.collect.ImmutableList;
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
import qbt.config.RepoConfig;
import qbt.diffmanifests.MapDiffer;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.vcs.CachedRemote;
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
                RepoConfig.RequireRepoRemoteResult lhsResult = config.repoConfig.requireRepoRemote(repo, lhs.version);
                RepoConfig.RequireRepoRemoteResult rhsResult = config.repoConfig.requireRepoRemote(repo, rhs.version);

                CachedRemote lhsRemote = lhsResult.getRemote();
                CachedRemote rhsRemote = rhsResult.getRemote();
                if(!lhsRemote.matchedLocal(rhsRemote)) {
                    throw new RuntimeException("Mismatched remotes: " + lhsRemote + " / " + rhsRemote);
                }
                LocalVcs localVcs = lhsRemote.getLocalVcs();

                try(QbtTempDir tempDir = new QbtTempDir()) {
                    Path dir = tempDir.path;
                    localVcs.createWorkingRepo(dir);
                    lhsRemote.findCommit(dir, ImmutableList.of(lhs.version));
                    rhsRemote.findCommit(dir, ImmutableList.of(rhs.version));

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
