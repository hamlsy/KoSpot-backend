package com.kospot.kospot.domain.coordinate.aop;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SidoRepository {
    Sido value() default Sido.NATIONWIDE;
//    String value() default "";
}
