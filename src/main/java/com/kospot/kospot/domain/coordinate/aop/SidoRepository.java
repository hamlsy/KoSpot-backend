package com.kospot.kospot.domain.coordinate.aop;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SidoRepository {
    Sido value() default Sido.NATIONWIDE;

}
