package com.kospot.infrastructure.lock.config;

import com.kospot.infrastructure.lock.strategy.HostAssignmentLockStrategy;
import com.kospot.infrastructure.lock.strategy.LuaScriptStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Lock Strategy Configuration
 * 
 * Lua Script 전략을 사용하여 방장 재지정 로직을 원자적으로 처리
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LockStrategyConfig {

    private final LuaScriptStrategy luaScriptStrategy;

    @Bean
    @Primary
    public HostAssignmentLockStrategy hostAssignmentLockStrategy() {
        log.info("Lock strategy initialized: {}", luaScriptStrategy.getStrategyName());
        return luaScriptStrategy;
    }
}
