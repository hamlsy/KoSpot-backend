package com.kospot.mvp.application.usecase;

import com.kospot.mvp.application.adaptor.DailyMvpAdaptor;
import com.kospot.mvp.domain.entity.DailyMvp;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.member.adaptor.MemberProfileRedisAdaptor;
import com.kospot.infrastructure.redis.domain.mvp.service.DailyMvpCacheService;
import com.kospot.mvp.presentation.response.DailyMvpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetDailyMvpUseCase {

    private final DailyMvpAdaptor dailyMvpAdaptor;
    private final DailyMvpCacheService dailyMvpCacheService;
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;

    public DailyMvpResponse.Daily execute(LocalDate date) {
        return dailyMvpCacheService.get(date)
                .orElseGet(() -> loadAndCache(date));
    }

    private DailyMvpResponse.Daily loadAndCache(LocalDate date) {
        if (dailyMvpCacheService.isNoneCached(date)) {
            return null;
        }

        if (!dailyMvpCacheService.tryAcquireRebuildLock(date)) {
            return dailyMvpCacheService.get(date).orElse(null);
        }

        try {
            DailyMvp dailyMvp = dailyMvpAdaptor.queryByDate(date).orElse(null);
            if (dailyMvp == null) {
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
}
