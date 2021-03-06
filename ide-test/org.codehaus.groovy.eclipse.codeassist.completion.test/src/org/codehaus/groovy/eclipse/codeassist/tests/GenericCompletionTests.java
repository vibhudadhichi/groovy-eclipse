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
 * Tests completions of generic lists, maps, etc.
 *
 * @author Andrew Eisenberg
 * @created Dec 20, 2010
 */
public final class GenericCompletionTests extends CompletionTestCase {

    public static Test suite() {
        return newTestSuite(FieldCompletionTests.class);
    }

    public void testAfterArrayAccesses1() throws Exception {
        String contents = "Map<String, Map<Integer, List<Date>>> dataTyped\ndataTyped      ['foo'].c\nj";
        String expected = "Map<String, Map<Integer, List<Date>>> dataTyped\ndataTyped      ['foo'].clear()\nj";
        checkProposalApplicationNonType(contents, expected, getIndexOf(contents, "['foo'].c"), "clear()");
    }

    public void testAfterArrayAccesses2() throws Exception {
        String contents = "Map<String, Map<Integer, List<Date>>> dataTyped\ndataTyped      ['foo'].";
        String expected = "Map<String, Map<Integer, List<Date>>> dataTyped\ndataTyped      ['foo'].clear()";
        checkProposalApplicationNonType(contents, expected, getIndexOf(contents, "['foo']."), "clear()");
    }

    public void testAfterMultipleArrayAccesses1() throws Exception {
        String contents = "Map<String, Map<Integer, List<Date>>> dataTyped\ndataTyped      ['foo'][5][2].t";
        String expected = "Map<String, Map<Integer, List<Date>>> dataTyped\ndataTyped      ['foo'][5][2].time";
        checkProposalApplicationNonType(contents, expected, getIndexOf(contents, "['foo'][5][2].t"), "time");
    }

    public void testAfterMultipleArrayAccesses2() throws Exception {
        String contents = "Map<String, Map<Integer, List<Date>>> dataTyped\ndataTyped      ['foo'][5][2].";
        String expected = "Map<String, Map<Integer, List<Date>>> dataTyped\ndataTyped      ['foo'][5][2].time";
        checkProposalApplicationNonType(contents, expected, getIndexOf(contents, "['foo'][5][2]."), "time");
    }
}
