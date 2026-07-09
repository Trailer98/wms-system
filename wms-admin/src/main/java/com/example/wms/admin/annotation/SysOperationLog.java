package com.example.wms.admin.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysOperationLog {
    String operationType();

    String content() default "";

    /** SpEL expression, evaluated against method args (by name) and {@code #result} after the method returns. */
    String bizNo() default "";

    String module() default "";

    /** Literal business type code (e.g. {@code INBOUND_ORDER}), not an expression. */
    String bizType() default "";

    /** SpEL expression for the business record id, same evaluation context as {@link #bizNo()}. */
    String bizId() default "";
}
