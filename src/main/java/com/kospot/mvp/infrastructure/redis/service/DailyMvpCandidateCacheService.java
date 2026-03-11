package com.kospot.mvp.infrastructure.redis.service;

import com.kospot.common.redis.common.constants.RedisKeyConstants;
import com.kospot.mvp.domain.vo.MvpCandidateSnapshot;
import com.kospot.mvp.infrastructure.redis.dao.DailyMvpCandidateRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DailyMvpCandidateCacheService {

    private static final Duration CANDIDATE_TTL = Duration.ofDays(2);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DailyMvpCandidateRedisRepository repository;

    public boolean compareAndSetIfBetter(LocalDate mvpDate, MvpCandidateSnapshot snapshot) {
        return repository.compareAndSetIfBetter(candidateKey(mvpDate), snapshot, CANDIDATE_TTL);
    }

    public Optional<MvpCandidateSnapshot> get(LocalDate mvpDate) {
        return repository.find(candidateKey(mvpDate));
    }

    private String candidateKey(LocalDate mvpDate) {
        return String.format(RedisKeyConstants.DAILY_MVP_CANDIDATE_KEY_PATTERN, mvpDate.format(DATE_FORMATTER));
    }
}
