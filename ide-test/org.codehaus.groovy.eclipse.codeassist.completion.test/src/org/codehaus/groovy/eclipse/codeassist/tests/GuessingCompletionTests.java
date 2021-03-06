/*
 * Copyright 2009-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.eclipse.codeassist.tests;

import junit.framework.Test;

/**
 * @author Andrew Eisenberg
 * @created Sep 9, 2011
 */
public final class GuessingCompletionTests extends CompletionTestCase {

    public static Test suite() {
        return newTestSuite(GuessingCompletionTests.class);
    }

    public void testParamGuessing1() throws Exception {
        String contents = "String yyy\n" +
                "def xxx(String x) { }\n" +
                "xxx";
        String[][] expectedChoices = new String[][] { new String[] { "yyy", "\"\"" } };
        checkProposalChoices(contents, "xxx", "xxx(yyy)", expectedChoices);
    }

    public void testParamGuessing2() throws Exception {
        String contents =
                "String yyy\n" +
                "int zzz\n" +
                "def xxx(String x, int z) { }\n" +
                "xxx";
        String[][] expectedChoices = new String[][] { new String[] { "yyy", "\"\"" }, new String[] { "zzz", "0" } };
        checkProposalChoices(contents, "xxx", "xxx(yyy, zzz)", expectedChoices);
    }

    public void testParamGuessing3() throws Exception {
        String contents =
                "String yyy\n" +
                "Integer zzz\n" +
                "boolean aaa\n" +
                "def xxx(String x, int z, boolean a) { }\n" +
                "xxx";
        String[][] expectedChoices = new String[][] { new String[] { "yyy", "\"\"" }, new String[] { "zzz", "0" }, new String[] { "aaa", "false", "true" } };
        checkProposalChoices(contents, "xxx", "xxx(yyy, zzz, aaa)", expectedChoices);
    }

    // GRECLIPSE-1268  This test may fail in some environments since the ordering of
    // guessed parameters is not based on actual source location.  Need a way to map
    // from variable name to local variable declaration in GroovyExtendedCompletionContext.computeVisibleElements(String)
    public void testParamGuessing4() throws Exception {
        String contents =
                "Closure yyy\n" +
                "def zzz = { }\n" +
                "def xxx(Closure c) { }\n" +
                "xxx";
        String[][] expectedChoices = {{"zzz", "yyy", "{  }"}};
        try {
            checkProposalChoices(contents, "xxx", "xxx {", expectedChoices);
        } catch (AssertionError e) {
            try {
                checkProposalChoices(contents, "xxx", "xxx yyy", expectedChoices);
            } catch (AssertionError e2) {
                checkProposalChoices(contents, "xxx", "xxx zzz", expectedChoices);
            }
        }
    }
}
