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
package qbt.recursive.cv;

import com.google.common.hash.HashCode;
import qbt.QbtHashUtils;
import qbt.TypedDigest;

public final class CumulativeVersionDigest extends TypedDigest {
    public CumulativeVersionDigest(HashCode delegate) {
        super(delegate);
    }

    // This should get changed (randomly) every time we make changes to the
    // fundamental action of QBT in a way that should force a rebuild of the
    // world.
    public static final CumulativeVersionDigest QBT_VERSION = new CumulativeVersionDigest(QbtHashUtils.parse("3eb9bfa6ca61dd44af98cea8ed7640a838606418"));
}
