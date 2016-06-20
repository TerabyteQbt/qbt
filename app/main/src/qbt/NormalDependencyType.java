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
package qbt;

public enum NormalDependencyType {
    STRONG("Strong", "S"),
    RUNTIME_WEAK("RuntimeWeak"),
    BUILDTIME_WEAK("Weak", "W"),
    ;

    private final String tag;
    private final String[] legacyTags;

    private NormalDependencyType(String tag, String... legacyTags) {
        this.tag = tag;
        this.legacyTags = legacyTags;
    }

    public String getTag() {
        return tag;
    }

    public static NormalDependencyType fromTag(String tag) {
        for(NormalDependencyType v : values()) {
            if(v.tag.equals(tag)) {
                return v;
            }
            for(String legacyTag : v.legacyTags) {
                if(legacyTag.equals(tag)) {
                    return v;
                }
            }
        }
        throw new IllegalArgumentException("No NormalDependencyType has tag: " + tag);
    }
}
