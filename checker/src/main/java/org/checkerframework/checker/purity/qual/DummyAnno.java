package org.checkerframework.checker.purity.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.*;

@DefaultQualifierInHierarchy
@InvisibleQualifier
@SubtypeOf({})
@Target({ElementType.TYPE_USE})
@TargetLocations({TypeUseLocation.EXPLICIT_UPPER_BOUND})
public @interface DummyAnno {}
