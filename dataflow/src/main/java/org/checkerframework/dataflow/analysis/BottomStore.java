package org.checkerframework.dataflow.analysis;

import javax.lang.model.type.TypeMirror;

/**
 * A store is used to keep track of the information that the org.checkerframework.dataflow analysis
 * has accumulated at any given point in time.
 *
 * @param <S> the type of the store returned by {@code copy} and that is used in {@code
 *     leastUpperBound}. Usually it is the implementing class itself, e.g. in {@code T extends
 *     Store<T>}.
 */
public interface BottomStore<V extends AbstractValue<V>, S extends BottomStore<V, S>> {

    boolean isBottom();

    S lubWithBottom();

    V getBottomValue(TypeMirror underlyingType);
}
