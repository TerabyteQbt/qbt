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
package qbt.tip;

public final class PackageTip extends AbstractTip<PackageTip> {
    private PackageTip(String name, String tip) {
        super(TYPE, name, tip);
    }

    public static final Type<PackageTip> TYPE = new Type<PackageTip>(PackageTip.class, "package") {
        @Override
        public PackageTip of(String name, String tip) {
            return new PackageTip(name, tip);
        }
    };
}
