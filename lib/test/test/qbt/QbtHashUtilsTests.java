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
import org.junit.Assert;
import org.junit.Test;

public class QbtHashUtilsTests {
    @Test
    public void testParse() {
        testParseFails("01");
        testParseSucceeds("0123456789012345678901234567890123456789");
        testParseSucceeds("012345678901234567890123456789012345678a");
        testParseSucceeds("012345678901234567890123456789012345678A");
        testParseFails("012345678901234567890123456789012345678X");
    }

    private void testParseFails(String arg) {
        try {
            QbtHashUtils.parse(arg);
            Assert.fail();
        }
        catch(IllegalArgumentException e) {
            // expected
        }
    }

    private void testParseSucceeds(String arg) {
        HashCode h1 = QbtHashUtils.parse(arg);
        HashCode h2 = QbtHashUtils.parse(arg);
        Assert.assertEquals(h1, h2);
        Assert.assertEquals(arg.toUpperCase(), h1.toString().toUpperCase());
    }
}
