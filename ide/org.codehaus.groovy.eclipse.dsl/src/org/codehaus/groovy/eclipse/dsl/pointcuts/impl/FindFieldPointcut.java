/*******************************************************************************
 * Copyright (c) 2011 Codehaus.org, SpringSource, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Andrew Eisenberg - Initial implemenation
 *******************************************************************************/
package org.codehaus.groovy.eclipse.dsl.pointcuts.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.eclipse.dsl.pointcuts.GroovyDSLDContext;

/**
 * the match returns true if the pattern passed in has a field with the
 * supplied characteristics (either a name, or another pointcut such as hasAnnotation).
 * @author andrew
 * @created Feb 11, 2011
 */
public class FindFieldPointcut extends FilteringPointcut<FieldNode> {

    public FindFieldPointcut(String containerIdentifier, String pointcutName) {
        super(containerIdentifier, pointcutName, FieldNode.class);
    }


    /**
     * extracts fields from the outer binding, or from the current type if there is no outer binding
     * the outer binding should be either a {@link Collection} or a {@link ClassNode}
     */
    protected List<FieldNode> filterOuterBindingByType(GroovyDSLDContext pattern) {
        Object outer = pattern.getOuterPointcutBinding();
        if (outer == null) {
            return pattern.getCurrentType().getFields();
        } else {
            if (outer instanceof Collection<?>) {
                List<FieldNode> fields = new ArrayList<FieldNode>();
                for (Object elt : (Collection<Object>) outer) {
                    if (elt instanceof FieldNode) {
                        fields.add((FieldNode) elt);
                    }
                }
                return fields;
            } else if (outer instanceof ClassNode) {
                return ((ClassNode) outer).getFields();
            }
        }
        return null;
    }
    
    
    @Override
    protected FieldNode filterObject(FieldNode result, GroovyDSLDContext context, String firstArgAsString) {
        if (firstArgAsString == null || result.getName().equals(firstArgAsString)) {
            return result;
        } else {
            return null;
        }
    }
}
