package com.kospot.domain.coordinate.aop;

import com.kospot.domain.coordinate.vo.Sido;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SidoRepository {
    Sido value() default Sido.NATIONWIDE;

}
