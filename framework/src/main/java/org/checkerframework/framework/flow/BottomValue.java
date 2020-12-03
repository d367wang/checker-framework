package org.checkerframework.framework.flow;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("unchecked")
public class BottomValue<V extends CFAbstractValue<V>> extends CFAbstractValue<V> {

    // protected final Set<AnnotationMirror> bottomAnnotations;

    protected BottomValue(
            CFAbstractAnalysis<V, ?, ?> analysis,
            Set<AnnotationMirror> annotations,
            TypeMirror underlyingType) {
        super(analysis, annotations, underlyingType);

        // QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
        // this.bottomAnnotations = (Set<AnnotationMirror>) hierarchy.getBottomAnnotations();

        /*
        this.bottomAnnotations = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : annotations) {
            annotations.add(hierarchy.getBottomAnnotation(anno));
        }
        */
    }

    @Override
    public TypeMirror getUnderlyingType() {
        return underlyingType;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof BottomValue)) {
            return false;
        }

        BottomValue<?> other = (BottomValue<?>) obj;
        return this.getUnderlyingType() == other.getUnderlyingType()
                || analysis.getTypes()
                        .isSameType(this.getUnderlyingType(), other.getUnderlyingType());
    }

    /**
     * Returns the string representation as a comma-separated list.
     *
     * @return the string representation as a comma-separated list
     */
    @Override
    public String toString() {
        return "BottomValue{"
                + "bottom annotations="
                + annotations
                + ", underlyingType="
                + underlyingType
                + '}';
    }

    @Override
    public V leastUpperBound(@Nullable V other) {
        return upperBound(other, false);
    }

    public V widenUpperBound(@Nullable V other) {
        return upperBound(other, true);
    }

    private V upperBound(@Nullable V other, boolean shouldWiden) {
        // If the other value is null or is bottom value as well,
        // the lub is the bottom value.
        if (other == null || other instanceof BottomValue) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }

        return other;
    }
}
