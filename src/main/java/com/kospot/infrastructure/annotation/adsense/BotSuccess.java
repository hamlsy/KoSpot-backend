package com.kospot.infrastructure.annotation.adsense;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BotSuccess {
    // Bot일때 가짜 데이터/ 성공 응답 반환
}
