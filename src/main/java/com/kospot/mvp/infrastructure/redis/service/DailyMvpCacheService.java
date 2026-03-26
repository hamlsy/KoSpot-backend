package com.kospot.mvp.infrastructure.redis.service;

import com.kospot.common.redis.common.constants.RedisKeyConstants;
import com.kospot.mvp.infrastructure.redis.dao.DailyMvpCacheRedisRepository;
import com.kospot.mvp.presentation.dto.response.DailyMvpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DailyMvpCacheService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Duration TODAY_TTL = Duration.ofMinutes(70);
    private static final Duration HISTORY_TTL = Duration.ofHours(24);
    private static final Duration NONE_TTL = Duration.ofMinutes(10);
    private static final Duration REWARD_DONE_TTL = Duration.ofDays(14);

    private final DailyMvpCacheRedisRepository repository;

    public Optional<DailyMvpResponse.Daily> get(LocalDate date) {
        return repository.find(cacheKey(date));
    }

    public void cache(LocalDate date, DailyMvpResponse.Daily response) {
        repository.save(cacheKey(date), response, resolveTtl(date));
        repository.delete(noneKey(date));
    }

    public boolean isNoneCached(LocalDate date) {
        return repository.existsNone(noneKey(date));
    }

    public void cacheNone(LocalDate date) {
        repository.delete(cacheKey(date));
        repository.saveNone(noneKey(date), NONE_TTL);
    }

    public void evict(LocalDate date) {
        repository.delete(cacheKey(date));
        repository.delete(noneKey(date));
    }

    public boolean tryAcquireRebuildLock(LocalDate date) {
        return repository.acquireLock(rebuildLockKey(date), Duration.ofSeconds(5));
    }

    public void releaseRebuildLock(LocalDate date) {
        repository.releaseLock(rebuildLockKey(date));
    }

    public boolean tryAcquireScheduleLock(String jobName, Duration ttl) {
        return repository.acquireLock(scheduleLockKey(jobName), ttl);
    }

    public void releaseScheduleLock(String jobName) {
        repository.releaseLock(scheduleLockKey(jobName));
    }

    public boolean tryAcquireRewardLock(LocalDate mvpDate, Long memberId, Duration ttl) {
        return repository.acquireLock(rewardLockKey(mvpDate, memberId), ttl);
    }

    public void releaseRewardLock(LocalDate mvpDate, Long memberId) {
        repository.releaseLock(rewardLockKey(mvpDate, memberId));
    }

    public boolean isRewardProcessed(LocalDate mvpDate, Long memberId) {
        return repository.exists(rewardDoneKey(mvpDate, memberId));
    }

    public void markRewardProcessed(LocalDate mvpDate, Long memberId) {
        repository.setIfAbsent(rewardDoneKey(mvpDate, memberId), "1", REWARD_DONE_TTL);
    }

    private Duration resolveTtl(LocalDate date) {
        LocalDate today = LocalDate.now(KST);
        return today.equals(date) ? TODAY_TTL : HISTORY_TTL;
    }

    private String cacheKey(LocalDate date) {
        return String.format(RedisKeyConstants.DAILY_MVP_KEY_PATTERN, formatDate(date));
    }

    private String noneKey(LocalDate date) {
        return String.format(RedisKeyConstants.DAILY_MVP_NONE_KEY_PATTERN, formatDate(date));
    }

    private String rebuildLockKey(LocalDate date) {
        return String.format(RedisKeyConstants.DAILY_MVP_REBUILD_LOCK_KEY_PATTERN, formatDate(date));
    }

    private String scheduleLockKey(String jobName) {
        return String.format(RedisKeyConstants.DAILY_MVP_SCHEDULE_LOCK_KEY_PATTERN, jobName);
    }

    private String rewardLockKey(LocalDate mvpDate, Long memberId) {
        return String.format(RedisKeyConstants.DAILY_MVP_REWARD_LOCK_KEY_PATTERN, formatDate(mvpDate), memberId);
    }

    private String rewardDoneKey(LocalDate mvpDate, Long memberId) {
        return String.format(RedisKeyConstants.DAILY_MVP_REWARD_DONE_KEY_PATTERN, formatDate(mvpDate), memberId);
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }
}
