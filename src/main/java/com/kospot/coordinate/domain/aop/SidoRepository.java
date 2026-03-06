package com.kospot.coordinate.domain.aop;

import com.kospot.coordinate.domain.entity.Sido;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SidoRepository {
    Sido value() default Sido.NATIONWIDE;

}
