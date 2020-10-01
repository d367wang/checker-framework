package org.checkerframework.checker.fenum;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

public class FenumVisitor extends BaseTypeVisitor<FenumAnnotatedTypeFactory> {

    /**
     * Map from the @Fenum value to the corresponding fake enum set. This map stores the constant
     * values in each fake enum set.
     */
    private Map<String, Set<Object>> fakeEnumValues;

    /**
     * Map from the @Fenum value to the annotated type of corresponding fake enum. This map stores
     * the type of each fake enum set, in order to check if the fake enum set contains multiple
     * constant type.
     */
    private Map<String, AnnotatedTypeMirror> fakeEnumAnnotatedType;

    public FenumVisitor(BaseTypeChecker checker) {
        super(checker);

        fakeEnumValues = new HashMap<>();
        fakeEnumAnnotatedType = new HashMap<>();
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
            if (isFakeEnumConstant(field)) {
                // If the field is a fake enum constant, validate the constants within the same fake
                // enum type.
                AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(field);
                String fenumVal = getFenumVal(atm.getAnnotation(Fenum.class));
                Object fieldVal = getConstant(field);
                assert fieldVal != null;

                if (fakeEnumValues.containsKey(fenumVal)) {
                    AnnotatedTypeMirror fakeEnumATM = fakeEnumAnnotatedType.get(fenumVal);
                    if (!atm.toString().equals(fakeEnumATM.toString())) {
                        checker.reportError(field, "multiple.underlying.types", fakeEnumATM, atm);

                    } else {
                        Set<Object> values = fakeEnumValues.get(fenumVal);
                        // if (values.contains(fieldVal)) {
                        if (containsValue(values, fieldVal)) {
                            checker.reportError(field, "duplicated.constant.value", fieldVal);
                        }
                    }
                } else {
                    fakeEnumValues.put(fenumVal, new HashSet<>());
                    fakeEnumValues.get(fenumVal).add(fieldVal);
                    fakeEnumAnnotatedType.put(fenumVal, atm);
                }
            }
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
    private Object getConstant(VariableTree node) {
        VariableElement varElement = TreeUtils.elementFromDeclaration(node);
        return ((VarSymbol) varElement).getConstValue();
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
    private String getFenumVal(AnnotationMirror anno) {
        return AnnotationUtils.getElementValue(anno, "value", String.class, false);
    }

    /**
     * Checks if the given variable is a fake enum constant.
     *
     * @param node the variable to test
     * @return true if the given variable is a fake enum constant
     */
    private boolean isFakeEnumConstant(VariableTree node) {
        VariableElement varElement = TreeUtils.elementFromDeclaration(node);
        AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(node);
        return hasFenumAnnotation(atm)
                && ElementUtils.isFinal(varElement)
                && ElementUtils.isStatic(varElement);
    }

    /**
     * Checks if a set contains certain value.
     *
     * @param values the set of values to test membership, the element of the set can be of
     *     arbitrary type
     * @param val the value to test membership
     * @return true if the set contains the specified value
     */
    private boolean containsValue(Set<Object> values, Object val) {
        for (Object v : values) {
            if ((val instanceof Number) && ((Number) val).equals((Number) v)) {
                return true;

            } else if ((val instanceof String) && ((String) val).equals((String) v)) {
                return true;

            } else if (val.equals(v)) {
                return true;
            }
        }
        return false;
    }
}
