package org.checkerframework.checker.purity;

import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.util.PurityChecker;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

public class PurityVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {
    private Map<String, Map<String, String>> purityGroups;

    public PurityVisitor(BaseTypeChecker checker) {
        super(checker);
        purityGroups = new HashMap<>();
    }

    public Map<String, Map<String, String>> getPurityGroups() {
        return purityGroups;
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        TreePath body = atypeFactory.getPath(node.getBody());
        PurityChecker.PurityResult r;
        if (body == null) {
            r = new PurityChecker.PurityResult();
        } else {
            r =
                    PurityChecker.checkPurity(
                            body,
                            atypeFactory,
                            checker.hasOption("assumeSideEffectFree")
                                    || checker.hasOption("assumePure"),
                            checker.hasOption("assumeDeterministic")
                                    || checker.hasOption("assumePure"));
        }

        EnumSet<Pure.Kind> purityKinds = r.getKinds();
        String purity;
        if (purityKinds.contains(Pure.Kind.SIDE_EFFECT_FREE)
                && purityKinds.contains(Pure.Kind.DETERMINISTIC)) {
            purity = "@dummy.purity.qual.Pure";
        } else if (purityKinds.contains(Pure.Kind.SIDE_EFFECT_FREE)
                && !purityKinds.contains(Pure.Kind.DETERMINISTIC)) {
            purity = "@dummy.purity.qual.SideEffectFree";
        } else if (!purityKinds.contains(Pure.Kind.SIDE_EFFECT_FREE)
                && purityKinds.contains(Pure.Kind.DETERMINISTIC)) {
            purity = "@dummy.purity.qual.Deterministic";
        } else {
            purity = "@dummy.purity.qual.Impure";
        }

        final ExecutableElement methodElem = TreeUtils.elementFromDeclaration(node);
        String methodName = methodElem.toString();
        TypeElement typeElement = ElementUtils.enclosingClass(methodElem);
        String className = typeElement.getQualifiedName().toString();

        if (!purityGroups.containsKey(className)) {
            purityGroups.put(className, new HashMap<>());
        }
        purityGroups.get(className).put(methodName, purity);

        return super.visitMethod(node, p);
    }
}
