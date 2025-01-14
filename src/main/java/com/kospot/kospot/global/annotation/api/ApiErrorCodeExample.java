package com.kospot.kospot.global.annotation.api;

import com.kospot.kospot.exception.payload.code.ErrorStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodeExample {
    ErrorStatus[] value() default {ErrorStatus._INTERNAL_SERVER_ERROR};
}
