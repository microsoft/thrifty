package com.bendb.thrifty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
