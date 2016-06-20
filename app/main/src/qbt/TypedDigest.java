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

import com.google.common.hash.HashCode;

public abstract class TypedDigest {
    HashCode delegate;

    protected TypedDigest(HashCode delegate) {
        this.delegate = delegate;
    }

    @Override
    public final boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(!getClass().equals(obj.getClass())) {
            return false;
        }
        TypedDigest other = (TypedDigest) obj;
        return delegate.equals(other.delegate);
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode() ^ delegate.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + delegate;
    }

    public HashCode getRawDigest() {
        return delegate;
    }
}
