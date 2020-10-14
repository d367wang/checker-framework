package org.checkerframework.checker.fenum;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.*;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.fenum.qual.FenumPattern;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

public class FenumVisitor extends BaseTypeVisitor<FenumAnnotatedTypeFactory> {

    /** Map from the @Fenum name to the corresponding {@link FenumSet}. */
    private Map<String, FenumSet> fakeEnumsMap;

    public FenumVisitor(BaseTypeChecker checker) {
        super(checker);
        fakeEnumsMap = new HashMap<>();
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        if (!TreeUtils.isStringConcatenation(node)) {
            // TODO: ignore string concatenations

            // The Fenum Checker is only concerned with primitive types, so just check that
            // the primary annotations are equivalent.
            AnnotatedTypeMirror lhsAtm = atypeFactory.getAnnotatedType(node.getLeftOperand());
            AnnotatedTypeMirror rhsAtm = atypeFactory.getAnnotatedType(node.getRightOperand());

            Set<AnnotationMirror> lhs = lhsAtm.getEffectiveAnnotations();
            Set<AnnotationMirror> rhs = rhsAtm.getEffectiveAnnotations();
            QualifierHierarchy qualHierarchy = atypeFactory.getQualifierHierarchy();
            if (!(qualHierarchy.isSubtype(lhs, rhs) || qualHierarchy.isSubtype(rhs, lhs))) {
                checker.reportError(node, "binary.type.incompatible", lhsAtm, rhsAtm);
            }
        }
        return super.visitBinary(node, p);
    }

    @Override
    public Void visitSwitch(SwitchTree node, Void p) {
        ExpressionTree expr = node.getExpression();
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(expr);

        for (CaseTree caseExpr : node.getCases()) {
            ExpressionTree realCaseExpr = caseExpr.getExpression();
            if (realCaseExpr != null) {
                AnnotatedTypeMirror caseType = atypeFactory.getAnnotatedType(realCaseExpr);

                this.commonAssignmentCheck(
                        exprType, caseType, caseExpr, "switch.type.incompatible");
            }
        }
        return super.visitSwitch(node, p);
    }

    @Override
    public void processClassTree(ClassTree classTree) {
        List<VariableTree> allFields = TreeUtils.getAllFields(classTree);
        for (VariableTree field : allFields) {
            if (isFenumDeclaration(field)) {
                // If the field is a fake enum constant, validate the constants within the same fake
                // enum type. If the validation succeeds, add the value of it to the fake enum set.
                // Otherwise an InvalidFenumConstantException will be raised
                try {
                    validateAndAddFenumValue(field);

                } catch (InvalidFenumConstantException e) {

                }
            }
        }

        // After all valid fake enum constants are grouped and loaded, check if the constant values
        // in each fake enum set are consecutive
        for (Map.Entry<String, FenumSet> entry : fakeEnumsMap.entrySet()) {
            FenumSet fenumSet = entry.getValue();

            if (!fenumSet.isValidated() && fenumSet.getPattern() == FenumPattern.CONSECUTIVE) {
                try {
                    validateConsecutivePattern(fenumSet);

                } catch (InvalidFenumConstantException e) {

                }
            }
            fenumSet.setValidated(true);
        }
        super.processClassTree(classTree);
    }

    @Override
    protected void checkConstructorInvocation(
            AnnotatedDeclaredType dt, AnnotatedExecutableType constructor, NewClassTree src) {
        // Ignore the default annotation on the constructor
    }

    @Override
    protected void checkConstructorResult(
            AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
        // Skip this check
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        return Collections.singleton(atypeFactory.FENUM_UNQUALIFIED);
    }

    // TODO: should we require a match between switch expression and cases?

    @Override
    public boolean isValidUse(
            AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        // The checker calls this method to compare the annotation used in a
        // type to the modifier it adds to the class declaration. As our default
        // modifier is FenumBottom, this results in an error when a non-subtype
        // is used. Can we use FenumTop as default instead?
        return true;
    }

    /**
     * Gets value of the variable which is declared to be constant. (i.e. final static variable)
     *
     * @param node the variable to get constant value from
     * @return the constant value of the variable
     */
    private Object getConstantValue(VariableTree node, TypeMirror underlyingType) {
        VariableElement varElement = TreeUtils.elementFromDeclaration(node);
        if (TypesUtils.isPrimitive(underlyingType)) {
            return varElement.getConstantValue();
        }
        final ExpressionTree initializer = node.getInitializer();
        assert initializer != null;
        if (initializer instanceof LiteralTree) {
            Object val = ((LiteralTree) initializer).getValue();
            return val instanceof String ? val : null;
        }
        return null;
    }

    /**
     * Checks if the given annotated type contains {@link Fenum} annotation.
     *
     * @param atm {@link AnnotatedTypeMirror} to test
     * @return true if {@code atm} contains {@link Fenum} annotation
     */
    private boolean hasFenumAnnotation(AnnotatedTypeMirror atm) {
        AnnotationMirror anno = atm.getAnnotation(Fenum.class);
        return anno != null;
    }

    /**
     * Gets {@code value} field of {@link Fenum} annotation.
     *
     * @param anno {@link Fenum} annotation
     * @return {@code value} option specified in the input annotation
     */
    private String getFenumName(AnnotationMirror anno) {
        return AnnotationUtils.getElementValue(anno, "value", String.class, false);
    }

    /**
     * Gets {@code value} field of {@link Fenum} annotation.
     *
     * @param anno {@link Fenum} annotation
     * @return {@code value} option specified in the input annotation
     */
    private FenumPattern getFenumPattern(AnnotationMirror anno) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> valmap;
        valmap = anno.getElementValues();

        for (ExecutableElement elem : valmap.keySet()) {
            if (elem.getSimpleName().contentEquals("pattern")) {
                AnnotationValue val = valmap.get(elem);
                switch (val.getValue().toString()) {
                    case "CONSECUTIVE":
                        return FenumPattern.CONSECUTIVE;
                    case "FLAG":
                        return FenumPattern.FLAG;
                }
            }
        }

        return FenumPattern.UNCHECKED;
    }

    /**
     * Checks if the given variable is a fake enum declaration.
     *
     * @param node the variable to test
     * @return true if the given variable is a fake enum declaration
     */
    private boolean isFenumDeclaration(VariableTree node) {
        VariableElement varElement = TreeUtils.elementFromDeclaration(node);
        AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(node);
        return hasFenumAnnotation(atm)
                && ElementUtils.isPublic(varElement)
                && ElementUtils.isFinal(varElement)
                && ElementUtils.isStatic(varElement);
    }

    /**
     * Adds a new integer constant into the corresponding fake enum set. If the fake enum set
     * already the constant value, issues an error and aborts.
     *
     * @param node the variable tree that identifies the new constant to be added
     * @param name the string that identified the fake enum type
     * @param value the value of the constant integer being added
     */
    private void validateAndAddFenumValue(VariableTree node) {
        // Get items of the variable that need to be validated, including the underlying type,
        // elements of the annotation, etc.
        AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(node);
        TypeMirror underlyingType = atm.getUnderlyingType();
        AnnotationMirror anno = atm.getAnnotation(Fenum.class);
        String fenumName = getFenumName(anno);
        FenumPattern fenumPattern = getFenumPattern(anno);
        Object constValue = getConstantValue(node, underlyingType);

        FenumSet fenumSet;
        if (fakeEnumsMap.containsKey(fenumName)) {
            // If the fake enum to add the constant already exists, validate the underlying type,
            // fenum pattern and constant value
            fenumSet = fakeEnumsMap.get(fenumName);
            validateUnderlyingType(node, fenumSet, underlyingType);
            validateFenumPattern(node, fenumSet, fenumPattern);
            validateValue(node, fenumSet, constValue);

        } else {
            // If the fake enum to add the constant does not exist, validate only the underlying
            // type. If succeeds, create the new corresponding fake enum set
            validateUnderlyingType(node, null, underlyingType);
            // underlying type is either int or String
            if (underlyingType.getKind() == TypeKind.INT) {
                fenumSet = new FenumSet(fenumName, FenumSet.TYPE_INTEGER, fenumPattern, node);

            } else {
                validateStringFenumPattern(node, fenumPattern);
                fenumSet = new FenumSet(fenumName, FenumSet.TYPE_STRING, fenumPattern, node);
            }

            // Validation passes (otherwise an InvalidFenumConstantException will be raised
            // halfway). Add the new constant value
            fakeEnumsMap.put(fenumName, fenumSet);
        }
        fenumSet.addValue(constValue);
    }

    /**
     * Validates the underlying type of a fake enum constant before it is added to the fake enum
     * set, and issue an error if the pattern is invalid.
     *
     * @param node the underlying fake enum constant of which the pattern is validated. This is used
     *     only for the source position in the error message
     * @param fenumSet the fake enum set to which the fake enum constant is added
     * @param tm the underlying type to be validated
     */
    private void validateUnderlyingType(VariableTree node, FenumSet fenumSet, TypeMirror tm) {
        if (!(tm.getKind() == TypeKind.INT || TypesUtils.isString(tm))) {
            reportErrorAndThrowException(node, "unsupported.constant.type", tm);
        }

        if (fenumSet != null) {
            if ((fenumSet.isInteger() && !(tm.getKind() == TypeKind.INT))
                    || (fenumSet.isString() && !(TypesUtils.isString(tm)))) {
                reportErrorAndThrowException(node, "constant.type.conflict", tm);
            }
        }
    }

    /**
     * Validates the pattern of a String-typed fake enum constant. The pattern is only allowed to be
     * {@link FenumPattern.UNCHECKED}
     *
     * @param node the underlying fake enum constant of which the pattern is validated. This is used
     *     only for the source position in the error message
     * @param fenumPattern the pattern to be validated
     */
    private void validateStringFenumPattern(VariableTree node, FenumPattern fenumPattern) {
        if (fenumPattern != FenumPattern.UNCHECKED) {
            reportErrorAndThrowException(node, "fenum.pattern.not.applicable", fenumPattern);
        }
    }

    /**
     * Validates the pattern of a fake enum constant before it is added to the fake enum set, and
     * issue an error if the pattern is invalid.
     *
     * @param node the underlying fake enum constant of which the pattern is validated. This is used
     *     only for the source position in the error message
     * @param fenumSet the fake enum set to which the fake enum constant is added
     * @param pattern the pattern to be validated
     */
    private void validateFenumPattern(VariableTree node, FenumSet fenumSet, FenumPattern pattern) {
        if (pattern != fenumSet.getPattern()) {
            reportErrorAndThrowException(
                    node, "fenum.pattern.conflict", fenumSet.getPattern(), pattern);
        }

        if (fenumSet.isString()) {
            validateStringFenumPattern(node, fenumSet.getPattern());
        }
    }

    /**
     * Validates the value of a fake enum constant before it is added to the fake enum set, and
     * issue an error if the value is invalid.
     *
     * @param node the underlying fake enum constant of which the value is validated. This is used
     *     only for the source position in the error message
     * @param fenumSet the fake enum set to which the fake enum constant is added
     * @param value the value to be validated
     */
    private void validateValue(VariableTree node, FenumSet fenumSet, Object value) {
        if (fenumSet.containsValue(value)) {
            reportErrorAndThrowException(
                    node, "duplicated.constant.value", value, fenumSet.getName());
        }

        if (fenumSet.getPattern() == FenumPattern.FLAG) {
            if (!isPowerOfTwo((int) value)) {
                reportErrorAndThrowException(node, "not.powerof.two", value);
            }
        }
    }

    /**
     * Tests if the given integer is a power of 2.
     *
     * @param value the integer to be tested
     * @return true if the integer is a power of 2
     */
    private boolean isPowerOfTwo(int value) {
        return (value & (value - 1)) == 0;
    }

    /**
     * Validates a consecutive-pattern fake enum set after all the constants are added, and issue an
     * error if the items in the fenum set are not consecutive.
     *
     * @param node one of the fake enum constants in the fake enum set to be validated. This is used
     *     only for the source position in the error message
     * @param fenumSet the fake enum set to be validated
     */
    private void validateConsecutivePattern(FenumSet fenumSet) {
        if (!fenumSet.isConsecutive()) {
            reportErrorAndThrowException(
                    fenumSet.getStart(), "nonconsecutive.constant.values", fenumSet.getName());
        }
    }

    private void reportErrorAndThrowException(VariableTree node, String errMsg, Object... args) {
        checker.reportError(node, errMsg, args);
        throw new InvalidFenumConstantException();
    }
}
