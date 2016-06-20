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
package qbt.artifactcacher;

public abstract class Architecture {
    private Architecture() {
        // hidden
    }

    public abstract <T> T visit(Visitor<T> visitor);

    public interface Visitor<T> {
        T visitUnknown();
        T visitIndependent();
        T visitNormal(String arch);
    }

    private static class UnknownArchitecture extends Architecture {
        @Override
        public <T> T visit(Visitor<T> visitor) {
            return visitor.visitUnknown();
        }

        @Override
        public String toString() {
            return "UnknownArchitecture";
        }
    }
    private static final Architecture UNKNOWN = new UnknownArchitecture();

    private static class IndependentArchitecture extends Architecture {
        @Override
        public <T> T visit(Visitor<T> visitor) {
            return visitor.visitIndependent();
        }

        @Override
        public String toString() {
            return "IndependentArchitecture";
        }
    }
    private static final Architecture INDEPENDENT = new IndependentArchitecture();

    private static class NormalArchitecture extends Architecture {
        private final String arch;

        public NormalArchitecture(String arch) {
            this.arch = arch;
        }

        @Override
        public <T> T visit(Visitor<T> visitor) {
            return visitor.visitNormal(arch);
        }

        @Override
        public String toString() {
            return "NormalArchitecture(" + arch + ")";
        }

        @Override
        public int hashCode() {
            return arch.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof NormalArchitecture)) {
                return false;
            }
            NormalArchitecture other = (NormalArchitecture)obj;
            return arch.equals(other.arch);
        }
    }

    public static Architecture unknown() {
        return UNKNOWN;
    }

    public static Architecture independent() {
        return INDEPENDENT;
    }

    public static Architecture fromArg(String arch) {
        if(arch == null) {
            return UNKNOWN;
        }
        // widen this if/when we care
        if(arch.matches("[a-zA-Z0-9]*")) {
            return new NormalArchitecture(arch);
        }
        throw new IllegalArgumentException("Invalid architecture: " + arch);
    }
}
