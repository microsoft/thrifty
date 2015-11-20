package com.bendb.thrifty;

import java.lang.annotation.*;

/**
 * Annotates generated fields with data necessary for serialization.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ThriftField {
    short fieldId();
    boolean isRequired() default false;
    String typedefName() default "";
}
