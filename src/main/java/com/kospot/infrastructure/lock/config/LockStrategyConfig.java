package com.kospot.infrastructure.lock.config;

import com.kospot.infrastructure.lock.strategy.HostAssignmentLockStrategy;
import com.kospot.infrastructure.lock.strategy.LuaScriptStrategy;
import com.kospot.infrastructure.lock.strategy.RedisTransactionStrategy;
import com.kospot.infrastructure.lock.strategy.RedissonLockStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Lock Strategy 선택 Configuration
 * 
 * application.yml에서 설정:
 * game-room:
 * lock-strategy: lua # redisson | transaction | lua
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LockStrategyConfig {

    @Value("${game-room.lock-strategy:redisson}")
    private String lockStrategy;

    private final RedissonLockStrategy redissonLockStrategy;
    private final RedisTransactionStrategy redisTransactionStrategy;
    private final LuaScriptStrategy luaScriptStrategy;

    @Bean
    @Primary
    public HostAssignmentLockStrategy hostAssignmentLockStrategy() {
        HostAssignmentLockStrategy strategy = switch (lockStrategy.toLowerCase()) {
            case "transaction" -> redisTransactionStrategy;
            case "lua" -> luaScriptStrategy;
            default -> redissonLockStrategy;
        };

        log.info("Lock strategy initialized: {} (configured: {})",
                strategy.getStrategyName(), lockStrategy);
        return strategy;
    }
}
