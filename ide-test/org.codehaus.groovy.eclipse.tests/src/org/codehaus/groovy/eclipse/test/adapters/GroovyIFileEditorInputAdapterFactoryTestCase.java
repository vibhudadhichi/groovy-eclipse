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
package org.codehaus.groovy.eclipse.test.adapters;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.eclipse.test.EclipseTestCase;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Tests the Groovy File Adapter Factory
 *
 * @author David Kerber
 */
public class GroovyIFileEditorInputAdapterFactoryTestCase extends EclipseTestCase {

    public void testIFileEditorInputAdapter() throws Exception {
        testProject.createGroovyTypeAndPackage("pack1", "MainClass.groovy",
            "class MainClass { static void main(String[] args");

        buildAll();

        final IFile script = (IFile) testProject.getProject().findMember("src/pack1/MainClass.groovy");

        assertNotNull(script);

        IFileEditorInput editor = new FileEditorInput(script);
        @SuppressWarnings("cast")
        ClassNode node = (ClassNode) editor.getAdapter(ClassNode.class);

        assertEquals("pack1.MainClass", node.getName());
        assertFalse(node.isInterface());
        assertNotNull(node.getMethods("main"));
    }

    public void testIFileEditorInputAdapterCompileError() throws Exception {
        testProject.createGroovyTypeAndPackage("pack1", "OtherClass.groovy",
            "class OtherClass { static void main(String[] args");

        buildAll();

        final IFile script = (IFile) testProject.getProject().findMember("src/pack1/OtherClass.groovy");
        assertNotNull(script);
        IFileEditorInput editor = new FileEditorInput(script);
        @SuppressWarnings("cast")
        ClassNode node = (ClassNode) editor.getAdapter(ClassNode.class);

        assertEquals("pack1.OtherClass", node.getName());
        assertFalse(node.isInterface());
        assertNotNull(node.getMethods("main"));
    }

    public void testIFileEditorInputAdapterHorendousCompileError() throws Exception {
        testProject.createFile("NotGroovy.file", "class C {\n abstract def foo() {}\n" + "}");

        buildAll();

        final IFile notScript = (IFile) testProject.getProject().findMember("src/NotGroovy.file");
        assertNotNull(notScript);
        IFileEditorInput editor = new FileEditorInput(notScript);
        assertNull(editor.getAdapter(ClassNode.class));
    }

    public void testIFileEditorInputAdapterNotGroovyFile() throws Exception {
        testProject.createFile("NotGroovy.file", "this is not a groovy file");

        buildAll();

        final IFile notScript = (IFile) testProject.getProject().findMember("src/NotGroovy.file");
        assertNotNull(notScript);
        IFileEditorInput editor = new FileEditorInput(notScript);
        assertNull(editor.getAdapter(ClassNode.class));
    }
}
