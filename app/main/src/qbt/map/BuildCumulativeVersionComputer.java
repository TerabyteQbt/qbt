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
package qbt.map;

import qbt.config.QbtConfig;
import qbt.manifest.current.QbtManifest;
import qbt.recursive.cv.CumulativeVersionNodeData;

// If you're just using the results to build then only CV matters.
public class BuildCumulativeVersionComputer extends CumulativeVersionComputer<CumulativeVersionNodeData> {
    public BuildCumulativeVersionComputer(QbtConfig config, QbtManifest manifest) {
        super(config, manifest);
    }

    @Override
    protected CumulativeVersionNodeData canonicalizationKey(CumulativeVersionComputer.Result result) {
        return result.cumulativeVersionNodeData;
    }
}
