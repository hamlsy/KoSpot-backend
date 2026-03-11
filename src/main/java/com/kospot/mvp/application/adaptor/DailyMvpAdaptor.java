package com.kospot.mvp.application.adaptor;

import com.kospot.mvp.domain.entity.DailyMvp;
import com.kospot.mvp.infrastructure.persistence.DailyMvpRepository;
import com.kospot.common.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Adaptor
@RequiredArgsConstructor
public class DailyMvpAdaptor {

    private final DailyMvpRepository repository;

    public Optional<DailyMvp> queryByDate(LocalDate date) {
        return repository.findByMvpDate(date);
    }

    public Optional<DailyMvp> queryByDateForUpdate(LocalDate date) {
        return repository.findByMvpDateForUpdate(date);
    }

    public List<DailyMvp> queryUnrewardedByDateLessThanEqual(LocalDate targetDate) {
        return repository.findUnrewardedByDateLessThanEqual(targetDate);
    }

    public DailyMvp save(DailyMvp dailyMvp) {
        return repository.save(dailyMvp);
    }
}
