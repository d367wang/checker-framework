package org.checkerframework.checker.fenum;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.TreeUtils;

public class FenumVisitor extends BaseTypeVisitor<FenumAnnotatedTypeFactory> {
    public FenumVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        AnnotatedTypeMirror lhsAtm = atypeFactory.getAnnotatedType(node.getLeftOperand());
        AnnotatedTypeMirror rhsAtm = atypeFactory.getAnnotatedType(node.getRightOperand());

        if (TreeUtils.typeOf(node).getKind().isPrimitive() && isValidBinaryOperation(node)) {
            // Fenum Checker only permits certain binary operations on primitive type
            Set<AnnotationMirror> lhs = lhsAtm.getEffectiveAnnotations();
            Set<AnnotationMirror> rhs = rhsAtm.getEffectiveAnnotations();
            QualifierHierarchy qualHierarchy = atypeFactory.getQualifierHierarchy();
            if (!(qualHierarchy.isSubtype(lhs, rhs) || qualHierarchy.isSubtype(rhs, lhs))) {
                checker.reportError(node, "binary.type.incompatible", lhsAtm, rhsAtm);
            }

        } else if (hasFenumAnnotation(lhsAtm) || hasFenumAnnotation(rhsAtm)) {
            checker.reportError(node, "binary.operation.unsupported", lhsAtm, rhsAtm);
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
     * Check if the binary operation {@code BinaryTree} is valid. Fenum Checker forbids binary
     * operations except comparison
     *
     * @param node the BinaryTree to test
     * @return true if the BinaryTree represents valid binary expression
     */
    private boolean isValidBinaryOperation(BinaryTree node) {
        Kind kind = node.getKind();
        return kind == Tree.Kind.EQUAL_TO
                || kind == Tree.Kind.NOT_EQUAL_TO
                || kind == Tree.Kind.LESS_THAN
                || kind == Tree.Kind.LESS_THAN_EQUAL
                || kind == Tree.Kind.GREATER_THAN
                || kind == Tree.Kind.GREATER_THAN_EQUAL;
    }

    /**
     * Check if the given annotated type contains {@link Fenum} annotation
     *
     * @param atm {@link AnnotatedTypeMirror} to test
     * @return true if {@code atm} contains {@link Fenum} annotation
     */
    private boolean hasFenumAnnotation(AnnotatedTypeMirror atm) {
        AnnotationMirror anno = atm.getAnnotation(Fenum.class);
        return anno != null;
    }
}
