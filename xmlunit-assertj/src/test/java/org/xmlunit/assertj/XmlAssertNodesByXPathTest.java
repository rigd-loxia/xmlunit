/*
  This file is licensed to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package org.xmlunit.assertj;

import org.junit.Rule;
import org.junit.Test;

import static java.lang.String.format;
import static org.xmlunit.assertj.ExpectedException.none;
import static org.xmlunit.assertj.XmlAssert.assertThat;

public class XmlAssertNodesByXPathTest {

    @Rule
    public ExpectedException thrown = none();

    @Test
    public void testXPath_shouldReturnNotNull() {
        final MultipleNodeAssert multipleNodeAssert1 = assertThat("<a><b></b><c/></a>").nodesByXPath("/a");
        final MultipleNodeAssert multipleNodeAssert2 = assertThat("<a><b></b><c/></a>").nodesByXPath("/x");

        AssertionsAdapter.assertThat(multipleNodeAssert1).isNotNull();
        AssertionsAdapter.assertThat(multipleNodeAssert2).isNotNull();
    }

    @Test
    public void testAssertThat_withNull_shouldFailed() {

        thrown.expectAssertionError(format("%nExpecting actual not to be null"));

        assertThat(null).nodesByXPath("//foo");
    }

    @Test
    public void testNodesByXPath_withNull_shouldFailed() {

        thrown.expectAssertionError(format("%nExpecting not blank but was:<null>"));

        assertThat("<a><b></b><c/></a>").nodesByXPath(null);
    }

    @Test
    public void testNodesByXPath_withWhitespacesOnly_shouldFailed() {

        thrown.expectAssertionError(format("%nExpecting not blank but was:<\" \n \t\">"));

        assertThat("<a><b></b><c/></a>").nodesByXPath(" \n \t");
    }

    @Test
    public void testNodesByXPath_withInvalidXML_shouldFailed() {

        thrown.expectAssertionError("Expecting code not to raise a throwable but caught");

        String xml = "<b>not empty</a>";

        assertThat(xml).nodesByXPath("//atom:feed/atom:entry/atom:id");
    }

    @Test
    public void nodesByXPath_withInvalidXPath_shouldFail() {

        thrown.expectAssertionError(format("%nExpecting code not to raise a throwable but caught"));

        assertThat("<a><b></b><c/></a>").nodesByXPath("this doesn't look like an XPath expression :-(");
    }

}
