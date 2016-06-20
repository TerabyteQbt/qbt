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
package qbt.mains;

import java.io.IOException;
import misc1.commons.options.OptionsFragment;
import misc1.commons.options.OptionsLibrary;
import misc1.commons.options.OptionsResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.HelpTier;
import qbt.QbtCommand;
import qbt.QbtCommandName;
import qbt.QbtCommandOptions;
import qbt.VcsVersionDigest;
import qbt.config.QbtConfig;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.options.ConfigOptionsDelegate;
import qbt.options.ManifestOptionsDelegate;
import qbt.options.ManifestOptionsResult;
import qbt.repo.LocalRepoAccessor;
import qbt.tip.RepoTip;
import qbt.vcs.Repository;

public final class AddRepository extends QbtCommand<AddRepository.Options> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddRepository.class);

    @QbtCommandName("addRepository")
    public static interface Options extends QbtCommandOptions {
        public static final OptionsLibrary<Options> o = OptionsLibrary.of();
        public static final ConfigOptionsDelegate<Options> config = new ConfigOptionsDelegate<Options>();
        public static final ManifestOptionsDelegate<Options> manifest = new ManifestOptionsDelegate<Options>();
        public final OptionsFragment<Options, String> repo = o.oneArg("repo").transform(o.singleton()).helpDesc("Repo to add");
    }

    @Override
    public Class<Options> getOptionsClass() {
        return Options.class;
    }

    @Override
    public HelpTier getHelpTier() {
        return HelpTier.UNCOMMON;
    }

    @Override
    public String getDescription() {
        return "add a repository to the manifest";
    }

    @Override
    public boolean isProgrammaticOutput() {
        return false;
    }

    @Override
    public int run(final OptionsResults<? extends Options> options) throws IOException {
        final QbtConfig config = Options.config.getConfig(options);
        final ManifestOptionsResult manifestResult = Options.manifest.getResult(options);
        QbtManifest manifest = manifestResult.parse();

        RepoTip newRepoTip = RepoTip.TYPE.parseRequire(options.get(Options.repo));

        // figure out the version of the new repo
        LocalRepoAccessor lra = config.localRepoFinder.findLocalRepo(newRepoTip);
        if(lra == null || !lra.isOverride()) {
            throw new IllegalArgumentException("Repository " + newRepoTip + " is not an override");
        }
        Repository repo = lra.vcs.getRepository(lra.dir);
        VcsVersionDigest currentCommit = repo.getCurrentCommit();
        RepoManifest.Builder rmb = RepoManifest.TYPE.builder().set(RepoManifest.VERSION, currentCommit);

        // create the pins
        config.localPinsRepo.addPin(newRepoTip, lra.dir, currentCommit);
        manifest = manifest.builder().with(newRepoTip, rmb).build();
        LOGGER.info("Added new repository " + newRepoTip + " (" + currentCommit.getRawDigest() + ") to manifest");
        // write out updated manifest
        manifestResult.deparse(manifest);
        return 0;
    }
}
