/*
 * Copyright 2009-2017 the original author or authors.
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
package org.codehaus.groovy.eclipse.core.inference;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.DynamicVariable;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.eclipse.core.compiler.CompilerUtils;
import org.codehaus.groovy.transform.stc.StaticTypesMarker;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.jdt.groovy.search.ITypeLookup;
import org.eclipse.jdt.groovy.search.TypeLookupResult;
import org.eclipse.jdt.groovy.search.TypeLookupResult.TypeConfidence;
import org.eclipse.jdt.groovy.search.VariableScope;

/**
 * @author Andrew Eisenberg
 */
public class STCTypeLookup implements ITypeLookup {

    // only enabled for Groovy 2.0 or greater
    private static final boolean isEnabled = (CompilerUtils.getActiveGroovyBundle().getVersion().getMajor() >= 2);

    public void initialize(GroovyCompilationUnit unit, VariableScope topLevelScope) {
    }

    public TypeLookupResult lookupType(Expression expr, VariableScope scope, ClassNode objectExpressionType) {
        if (!isEnabled) {
            return null;
        }
        Object inferredType = expr.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
        TypeConfidence confidence = TypeConfidence.INFERRED;
        ASTNode declaration = expr;

        if (expr instanceof VariableExpression) {
            Variable accessedVariable = ((VariableExpression) expr).getAccessedVariable();
            if (accessedVariable instanceof ASTNode) {
                declaration = (ASTNode) accessedVariable;
            } else if (accessedVariable instanceof DynamicVariable) {
                // defer to other type lookup impls
                confidence = TypeConfidence.UNKNOWN;
            }
        } else if (!(inferredType instanceof ClassNode) && // check for VariableExpressionTransformer's substitution
                expr instanceof ConstantExpression && (declaration = scope.getEnclosingNode()) instanceof PropertyExpression) {
            Object call = declaration.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
            if (call instanceof MethodNode) {
                declaration = (MethodNode) call;
                inferredType = ((MethodNode) call).getReturnType();
                objectExpressionType = ((MethodNode) call).getDeclaringClass();
            }
        } else if (expr instanceof FieldExpression) {
            declaration = ((FieldExpression) expr).getField();
        } else if (expr instanceof ClassExpression) {
            declaration = expr.getType();
        }

        if (inferredType instanceof ClassNode) {
            return new TypeLookupResult((ClassNode) inferredType, objectExpressionType, declaration, confidence, scope);
        }
        return null;
    }

    public TypeLookupResult lookupType(FieldNode node, VariableScope scope) {
        if (!isEnabled) {
            return null;
        }
        Object inferredType = node.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
        if (inferredType instanceof ClassNode) {
            return new TypeLookupResult((ClassNode) inferredType, node.getDeclaringClass(), node, TypeConfidence.INFERRED, scope);
        }
        return null;
    }

    public TypeLookupResult lookupType(MethodNode node, VariableScope scope) {
        if (!isEnabled) {
            return null;
        }
        Object inferredType = node.getNodeMetaData(StaticTypesMarker.INFERRED_RETURN_TYPE);
        if (inferredType instanceof ClassNode) {
            return new TypeLookupResult((ClassNode) inferredType, node.getDeclaringClass(), node, TypeConfidence.INFERRED, scope);
        }
        return null;
    }

    public TypeLookupResult lookupType(AnnotationNode node, VariableScope scope) {
        return null;
    }

    public TypeLookupResult lookupType(ImportNode node, VariableScope scope) {
        return null;
    }

    public TypeLookupResult lookupType(ClassNode node, VariableScope scope) {
        return null;
    }

    public TypeLookupResult lookupType(Parameter node, VariableScope scope) {
        return null;
    }
}
