package com.kospot.infrastructure.doc.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebSocketDoc {
    String destination();
    String description() default "";
    Class<?> payloadType();
    String trigger() default ""; // 어떤 이벤트에 발생하는지
}
