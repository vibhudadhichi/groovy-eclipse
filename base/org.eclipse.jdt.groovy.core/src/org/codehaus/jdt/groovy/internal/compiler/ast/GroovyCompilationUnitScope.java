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
package org.codehaus.jdt.groovy.internal.compiler.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.codehaus.groovy.ast.ClassNode;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.ImportBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.core.builder.AbortIncrementalBuildException;
import org.eclipse.jdt.internal.core.builder.NameEnvironment;

/**
 * A subtype of CompilationUnitScope that allows us to override some methods and prevent JDT doing some checks that groovy will be
 * doing anyway (or that JDT should be prevented from doing on groovy type declarations)
 *
 * @author Andy Clement
 */
public class GroovyCompilationUnitScope extends CompilationUnitScope {

    private static final char[][] javaLang;
    // Matches ResolveVisitor - these are the additional automatic imports for groovy files
    private static final char[][] javaIo;
    private static final char[][] javaNet;
    private static final char[][] javaUtil;
    private static final char[][] groovyLang;
    private static final char[][] groovyUtil;

    private static final char[][] javaMathBigDecimal;
    private static final char[][] javaMathBigInteger;

    static {
        javaLang = CharOperation.splitOn('.', "java.lang".toCharArray());
        javaIo = CharOperation.splitOn('.', "java.io".toCharArray());
        javaNet = CharOperation.splitOn('.', "java.net".toCharArray());
        javaUtil = CharOperation.splitOn('.', "java.util".toCharArray());
        groovyLang = CharOperation.splitOn('.', "groovy.lang".toCharArray());
        groovyUtil = CharOperation.splitOn('.', "groovy.util".toCharArray());

        javaMathBigDecimal = CharOperation.splitOn('.', "java.math.BigDecimal".toCharArray());
        javaMathBigInteger = CharOperation.splitOn('.', "java.math.BigInteger".toCharArray());
    }

    private final static char[] GROOVY = "groovy".toCharArray();
    private final static char[] LANG = "lang".toCharArray();
    private final static char[][] GROOVY_LANG_GROOVYOBJECT = { GROOVY, LANG, "GroovyObject".toCharArray() };

    private boolean isScript = false;

    public GroovyCompilationUnitScope(GroovyCompilationUnitDeclaration compilationUnitDeclaration, LookupEnvironment lookupEnvironment) {
        super(compilationUnitDeclaration, lookupEnvironment);
        INameEnvironment nameEnvironment = lookupEnvironment.nameEnvironment;
        if (nameEnvironment instanceof NameEnvironment) {
            ((NameEnvironment) nameEnvironment).avoidAdditionalGroovyAnswers = true;
        }
    }

    @Override
    protected ImportBinding[] getDefaultImports() {
        List<ImportBinding> importBindings = new ArrayList<ImportBinding>();

        Collections.addAll(importBindings, super.getDefaultImports()); // picks up 'java.lang'

        // augment with the groovy additional automatic imports
        Binding importBinding = environment.createPackage(javaIo);
        importBindings.add(new ImportBinding(javaIo, true, importBinding, null));

        importBinding = environment.createPackage(javaNet);
        importBindings.add(new ImportBinding(javaNet, true, importBinding, null));

        importBinding = environment.createPackage(javaUtil);
        importBindings.add(new ImportBinding(javaUtil, true, importBinding, null));

        importBinding = environment.createPackage(groovyLang);
        importBindings.add(new ImportBinding(groovyLang, true, importBinding, null));

        importBinding = environment.createPackage(groovyUtil);
        importBindings.add(new ImportBinding(groovyUtil, true, importBinding, null));

        // and specific imports for BigDecimal and BigInteger
        Binding jmBigDecimal = environment.getType(javaMathBigDecimal);
        importBindings.add(new ImportBinding(javaMathBigDecimal, false, jmBigDecimal, null));

        Binding jmBigInteger = environment.getType(javaMathBigInteger);
        importBindings.add(new ImportBinding(javaMathBigInteger, false, jmBigInteger, null));

        CompilerOptions co = compilerOptions();
        String extraImports = co.groovyExtraImports;
        // TODO support static imports
        // TODO need to refactor (code is copied in JDTResolver)
        if (extraImports != null) {
            try {
                String filename = new String(referenceContext.getFileName());
                // may be something to do
                StringTokenizer st = new StringTokenizer(extraImports, ";");
                // Form would be 'com.foo.*,com.bar.MyType;.gradle=com.this.*,com.foo.Type"
                // If there is no qualifying suffix it applies to all types

                while (st.hasMoreTokens()) {
                    String onesuffix = st.nextToken();
                    int equals = onesuffix.indexOf('=');
                    @SuppressWarnings("unused")
                    boolean shouldApply = false;
                    String imports = null;
                    if (equals == -1) {
                        // definetly applies
                        shouldApply = true;
                        imports = onesuffix;
                    } else {
                        // need to check the suffix
                        String suffix = onesuffix.substring(0, equals);
                        shouldApply = filename.endsWith(suffix);
                        imports = onesuffix.substring(equals + 1);
                    }
                    StringTokenizer st2 = new StringTokenizer(imports, ",");
                    while (st2.hasMoreTokens()) {
                        String nextElement = st2.nextToken();
                        // One of two forms: a.b.c.* or a.b.c.Type
                        if (nextElement.endsWith(".*")) {
                            char[] withoutDotStar = nextElement.substring(0, nextElement.length() - 2).toCharArray();
                            char[][] cs = CharOperation.splitOn('.', withoutDotStar);
                            importBinding = environment.createPackage(cs);
                            // TODO verify binding exists!
                            importBindings.add(new ImportBinding(cs, true, importBinding, null));
                        } else {
                            int asIndex = nextElement.indexOf(" as ");
                            String asName = null;

                            if (asIndex != -1) {
                                asName = nextElement.substring(asIndex + 4).trim();
                                nextElement = nextElement.substring(0, asIndex).trim();
                            }
                            char[] type = nextElement.toCharArray();
                            char[][] cs = CharOperation.splitOn('.', type);
                            Binding typeBinding = environment.getType(cs);
                            importBindings.add(new ImportBinding(cs, false, typeBinding, null));
                            if (asName != null) {
                                char[] asNameChars = asName.toCharArray();
                                char[][] cs2 = new char[1][];
                                cs2[0] = asNameChars;
                                importBindings.add(new ImportBinding(cs2, false, typeBinding, null));
                            }
                        }
                    }

                }
            } catch (Exception e) {
                new RuntimeException("Problem processing extraImports: " + extraImports, e).printStackTrace();
            }
        }

        /* See https://github.com/groovy/groovy-eclipse/issues/256 and https://issues.apache.org/jira/browse/GROOVY-8063
         *
         * @interface Anno { Class value() }
         *
         * @Anno(value=Inner) // Inner cannot be resolved -- I think this is correct behavior; below enables resolution
         * class Outer {
         *   static class Inner {}
         * }
         */
        //for (SourceTypeBinding topLevelType : topLevelTypes) {
        //    if (topLevelType.hasMemberTypes()) // add synthetic import to help resolve inner types
        //        importBindings.add(new ImportBinding(topLevelType.compoundName, true, topLevelType, null));
        //}

        return importBindings.toArray(new ImportBinding[importBindings.size()]);
    }

    @Override
    protected ClassScope buildClassScope(Scope parent, TypeDeclaration typeDecl) {
        return new GroovyClassScope(parent, typeDecl);
    }

    /**
     * Called after the type hierarchy is built for all types involved - allows glObject to be inserted
     */
    @Override
    public void augmentTypeHierarchy() {
        for (SourceTypeBinding topLevelType : topLevelTypes) {
            augmentTypeHierarchy(topLevelType);
        }
    }

    // FIXASC move this into GroovyClassScope
    /**
     * Ensure Groovy types extend groovy.lang.GroovyObject
     */
    private void augmentTypeHierarchy(SourceTypeBinding typeBinding) {
        if (typeBinding.isAnnotationType() || typeBinding.isInterface()) {
            return;
        }
        ReferenceBinding groovyLangObjectBinding = getGroovyLangObjectBinding();
        if (!typeBinding.implementsInterface(groovyLangObjectBinding, true)) {
            ReferenceBinding[] superInterfaceBindings = typeBinding.superInterfaces;
            if (superInterfaceBindings != null) {
                int count = superInterfaceBindings.length;
                System.arraycopy(superInterfaceBindings, 0, superInterfaceBindings = new ReferenceBinding[count + 1], 0, count);
                superInterfaceBindings[count] = groovyLangObjectBinding;
                typeBinding.superInterfaces = superInterfaceBindings;
            }
        }
    }

    private final ReferenceBinding getGroovyLangObjectBinding() {
        CompilationUnitScope unitScope = compilationUnitScope();
        unitScope.recordQualifiedReference(GROOVY_LANG_GROOVYOBJECT);
        return unitScope.environment.getResolvedType(GROOVY_LANG_GROOVYOBJECT, this);
    }

    @Override
    protected void buildTypeBindings(AccessRestriction accessRestriction) {
        TypeDeclaration[] types = referenceContext.types;
        if (types != null) {
            for (TypeDeclaration type : types) {
                if (type instanceof GroovyTypeDeclaration) {
                    ((GroovyTypeDeclaration) type).fixAnonymousTypeBinding(this);
                }
            }
        }
        super.buildTypeBindings(accessRestriction);
    }

    /*
     * Not quite the right name for this method, because on an incremental build
     * it will find BinaryTypeBindings for types that were SourceTypeBindings
     * during the full build
     */
    public ClassNode lookupClassNodeForSource(String typename, JDTResolver jdtResolver) {
        char[][] compoundName = CharOperation.splitOn('.', typename.toCharArray());
        TypeBinding jdtBinding = null;
        try {
            jdtBinding = getType(compoundName, compoundName.length);
        } catch (AbortCompilation t) {
            if (t.silentException instanceof AbortIncrementalBuildException) {
                jdtBinding = null;
            } else {
                throw t;
            }
        }

        if (jdtBinding instanceof SourceTypeBinding || jdtBinding instanceof BinaryTypeBinding) {
            return jdtResolver.convertToClassNode(jdtBinding);
        }

        // FIXASC better to look it up properly as a member type rather than catch the problem and unwrap!
        if (jdtBinding instanceof ProblemReferenceBinding) {
            ProblemReferenceBinding prBinding = (ProblemReferenceBinding) jdtBinding;
            if (prBinding.problemId() == ProblemReasons.InternalNameProvided) {
                jdtBinding = prBinding.closestMatch();
                if (jdtBinding instanceof SourceTypeBinding || jdtBinding instanceof BinaryTypeBinding) {
                    return jdtResolver.convertToClassNode(jdtBinding);
                }
            }
        }

        return null;
    }

    public ClassNode lookupClassNodeForBinary(String typename, JDTResolver jdtResolver) {
        char[][] compoundName = CharOperation.splitOn('.', typename.toCharArray());
        TypeBinding jdtBinding = getType(compoundName, compoundName.length);

        if (jdtBinding instanceof BinaryTypeBinding) {
            return jdtResolver.convertToClassNode(jdtBinding);
        }

        if (jdtBinding instanceof ProblemReferenceBinding) {
            ProblemReferenceBinding prBinding = (ProblemReferenceBinding) jdtBinding;
            if (prBinding.problemId() == ProblemReasons.InternalNameProvided) {
                jdtBinding = prBinding.closestMatch();
                if (jdtBinding instanceof BinaryTypeBinding) {
                    return jdtResolver.convertToClassNode(jdtBinding);
                }
            }
        }

        return null;
    }

    @Override
    protected void checkPublicTypeNameMatchesFilename(TypeDeclaration typeDecl) {
    }

    @Override
    protected void recordImportProblem(ImportReference importReference, Binding importBinding) {
    }

    @Override
    protected boolean reportPackageIsNotExpectedPackage(CompilationUnitDeclaration compUnitDecl) {
        // Code that could be used to police package declarations.
        // Rule: if there is a package declaration it must match the location on disk. If
        // there is no package declaration, let them get away with it

        // where we should be:
        if (!isScript && compUnitDecl != null && compUnitDecl.compilationResult != null
                && compUnitDecl.compilationResult.compilationUnit != null) {
            char[][] packageName = compUnitDecl.compilationResult.compilationUnit.getPackageName();
            String shouldBe = packageName == null ? "" : CharOperation.toString(packageName);
            // where we are declared:
            String actuallyIs = compUnitDecl.currentPackage == null ? "" : CharOperation
                    .toString(compUnitDecl.currentPackage.tokens);
            if (actuallyIs.length() > 0 && !shouldBe.equals(actuallyIs)) {
                problemReporter().packageIsNotExpectedPackage(compUnitDecl);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void checkParameterizedTypes() {
    }

    @Override
    public boolean reportInvalidType(TypeReference typeReference, TypeBinding resolvedType) {
        if (resolvedType instanceof ProblemReferenceBinding) {
            ProblemReferenceBinding problemRefBinding = (ProblemReferenceBinding) resolvedType;
            if (problemRefBinding.problemId() == ProblemReasons.Ambiguous) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void reportImportProblem(ImportReference importReference, Binding importBinding) {
        // GRE-680
        if (importBinding instanceof ProblemReferenceBinding) {
            ProblemReferenceBinding problemRefBinding = (ProblemReferenceBinding) importBinding;
            if (problemRefBinding.problemId() == ProblemReasons.NotFound) {
                return;
            }
        }
        problemReporter().importProblem(importReference, importBinding);
    }

    @Override
    public boolean canSeeEverything() {
        return true;
    }

    @Override
    public boolean checkTargetCompatibility() {
        return false;
    }

    @Override
    protected boolean canBeSeenBy(ReferenceBinding type, PackageBinding fPackage) {
        return true;
    }

    @Override
    public boolean scannerAvailable() {
        return false;
    }

    public void setIsScript(boolean isScript) {
        this.isScript = isScript;
    }

    public boolean isScript() {
        return isScript;
    }

    /**
     * This method is designed to be called when two bindings have been discovered, it will determine which is the right answer or
     * return null if there is no right answer (and an ambiguous binding message will be reported). If in here it means two star
     * imports have found a type. One might be a groovy.util style input (i.e. a 'built in' import), and one a 'normal' import that
     * was actually expressed in the source code. Whether the newly found type was discovered via an import expressed in the import
     * is determined by the 'isDeclaredImport' flag. If that is true we just have to check whether the originally found type uses
     * one of the special names. If the original type doesn't use a 'special name' then we allow it to override the newly found
     * value and return it.<br>
     * This code does not yet allow for the originallyFound import to be also found via a declared import (e.g. if the user is daft
     * enough to 'import groovy.util.*' - making a change to pass that information through would be more disruptive.
     *
     * @param newlyFound the binding found after the first one was discovered
     * @param originallyFound the binding found initially
     * @param isDeclaredImport indicates if the 'temp' binding was found using a real import from the source code (rather than an
     *        'injected' one)
     */
    public ReferenceBinding selectBinding(ReferenceBinding newlyFound, ReferenceBinding originallyFound, boolean isDeclaredImport) {
        if (isDeclaredImport) {
            // This means 'temp' is found via a real import reference in the source code, let's take a closer look at 'type'
            if (originallyFound.fPackage != null) {
                char[][] packageName = originallyFound.fPackage.compoundName;
                // packageName might be 'groovy.util'
                if (CharOperation.equals(javaLang, packageName) || CharOperation.equals(javaIo, packageName)
                        || CharOperation.equals(javaNet, packageName) || CharOperation.equals(javaUtil, packageName)
                        || CharOperation.equals(groovyLang, packageName) || CharOperation.equals(groovyUtil, packageName)) {
                    return newlyFound;
                } else {
                    // Groovy rule: if the originally found one is via a declared import, which it must be
                    // if we are here, use it in preference to the newlyFound one.
                    return originallyFound;
                }
            }
        }
        return null;
    }
}
