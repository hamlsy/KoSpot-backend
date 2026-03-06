package com.kospot.domain.mvp.adaptor;

import com.kospot.domain.mvp.entity.DailyMvp;
import com.kospot.domain.mvp.repository.DailyMvpRepository;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
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
