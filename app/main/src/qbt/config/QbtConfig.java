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
package qbt.config;

import groovy.lang.GroovyShell;
import java.nio.file.Path;
import misc1.commons.ExceptionUtils;
import qbt.VcsVersionDigest;
import qbt.artifactcacher.ArtifactCacher;
import qbt.repo.CommonRepoAccessor;
import qbt.repo.LocalRepoAccessor;
import qbt.tip.RepoTip;

public final class QbtConfig {
    public final LocalRepoFinder localRepoFinder;
    public final LocalPinsRepo localPinsRepo;
    public final QbtRemoteFinder qbtRemoteFinder;
    public final ArtifactCacher artifactCacher;

    public QbtConfig(LocalRepoFinder localRepoFinder, LocalPinsRepo localPinsRepo, QbtRemoteFinder qbtRemoteFinder, ArtifactCacher artifactCacher) {
        this.localRepoFinder = localRepoFinder;
        this.localPinsRepo = localPinsRepo;
        this.qbtRemoteFinder = qbtRemoteFinder;
        this.artifactCacher = artifactCacher;
    }

    public static QbtConfig parse(Path f) {
        GroovyShell shell = new GroovyShell();
        shell.setVariable("workspaceRoot", f.getParent());
        try {
            return (QbtConfig) shell.evaluate(f.toFile());
        }
        catch(Exception e) {
            throw ExceptionUtils.commute(e);
        }
    }

    public CommonRepoAccessor requireCommonRepo(RepoTip repo, VcsVersionDigest version) {
        LocalRepoAccessor local = localRepoFinder.findLocalRepo(repo);
        if(local != null) {
            return local;
        }
        return localPinsRepo.requirePin(repo, version, "Could not find override or local pin for " + repo + " at " + version);
    }
}
