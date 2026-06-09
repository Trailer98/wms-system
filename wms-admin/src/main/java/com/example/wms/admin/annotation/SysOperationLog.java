package com.example.wms.admin.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysOperationLog {
    String operationType();

    String content() default "";

    String bizNo() default "";
}
