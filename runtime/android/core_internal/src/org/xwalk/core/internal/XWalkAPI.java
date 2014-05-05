package org.xwalk.core.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface XWalkAPI {
    Class<?> impl() default Object.class;
    Class<?> instance() default Object.class;
    boolean createInternally() default false;
    boolean createExternally() default false;
    boolean noInstance() default false;
    boolean isConst() default false;
    Class<?> extendClass() default Object.class;
    String[] preWrapperLines() default {};
    String[] postWrapperLines() default {};
}
