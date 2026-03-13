package com.kospot.mvp.application.usecase;

import com.kospot.mvp.application.adaptor.DailyMvpAdaptor;
import com.kospot.mvp.application.service.DailyMvpReconcileService;
import com.kospot.mvp.domain.entity.DailyMvp;
import com.kospot.mvp.domain.vo.MvpCandidateSnapshot;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.mvp.infrastructure.redis.service.DailyMvpCandidateCacheService;
import com.kospot.mvp.infrastructure.redis.service.DailyMvpCacheService;
import com.kospot.mvp.presentation.response.DailyMvpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetDailyMvpUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DailyMvpAdaptor dailyMvpAdaptor;
    private final DailyMvpCacheService dailyMvpCacheService;
    private final DailyMvpCandidateCacheService dailyMvpCandidateCacheService;
    private final DailyMvpReconcileService dailyMvpReconcileService;
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;

    public DailyMvpResponse.Daily execute(LocalDate date) {
        return dailyMvpCacheService.get(date)
                .orElseGet(() -> loadAndCache(date));
    }

    private DailyMvpResponse.Daily loadAndCache(LocalDate date) {
        if (dailyMvpCacheService.isNoneCached(date)) {
            if (!isToday(date)) {
                return null;
            }

            Optional<DailyMvpResponse.Daily> candidateResponse = loadFromCandidate(date);
            if (candidateResponse.isPresent()) {
                dailyMvpCacheService.cache(date, candidateResponse.get());
                triggerTodayReconcile(date);
                return candidateResponse.get();
            }

            return null;
        }

        if (!dailyMvpCacheService.tryAcquireRebuildLock(date)) {
            DailyMvpResponse.Daily cached = dailyMvpCacheService.get(date).orElse(null);
            if (cached != null) {
                return cached;
            }
            return isToday(date) ? loadFromCandidate(date).orElse(null) : null;
        }

        try {
            DailyMvp dailyMvp = dailyMvpAdaptor.queryByDate(date).orElse(null);
            if (dailyMvp == null) {
                Optional<DailyMvpResponse.Daily> candidateResponse = isToday(date) ? loadFromCandidate(date) : Optional.empty();
                if (candidateResponse.isPresent()) {
                    dailyMvpCacheService.cache(date, candidateResponse.get());
                    triggerTodayReconcile(date);
                    return candidateResponse.get();
                }

                dailyMvpCacheService.cacheNone(date);
                return null;
            }

            MemberProfileRedisAdaptor.MemberProfileView profileView = memberProfileRedisAdaptor.findProfile(dailyMvp.getMemberId());
            DailyMvpResponse.Daily response = DailyMvpResponse.Daily.from(dailyMvp, profileView);
            dailyMvpCacheService.cache(date, response);
            return response;
        } finally {
            dailyMvpCacheService.releaseRebuildLock(date);
        }
    }

    private Optional<DailyMvpResponse.Daily> loadFromCandidate(LocalDate date) {
        return dailyMvpCandidateCacheService.get(date)
                .map(snapshot -> toCandidateResponse(date, snapshot));
    }

    private DailyMvpResponse.Daily toCandidateResponse(LocalDate date, MvpCandidateSnapshot snapshot) {
        MemberProfileRedisAdaptor.MemberProfileView profileView = memberProfileRedisAdaptor.findProfile(snapshot.memberId());
        return DailyMvpResponse.Daily.from(date, snapshot, profileView);
    }

    private void triggerTodayReconcile(LocalDate date) {
        try {
            dailyMvpReconcileService.reconcileByDateInNewTransaction(date);
        } catch (Exception e) {
            log.warn("Failed to reconcile today MVP after candidate fallback. date={}", date, e);
        }
    }

    private boolean isToday(LocalDate date) {
        return LocalDate.now(KST).equals(date);
    }
}
