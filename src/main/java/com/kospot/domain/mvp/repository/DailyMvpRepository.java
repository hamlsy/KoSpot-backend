package com.kospot.domain.mvp.repository;

import com.kospot.domain.mvp.entity.DailyMvp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyMvpRepository extends JpaRepository<DailyMvp, Long> {

    Optional<DailyMvp> findByMvpDate(LocalDate mvpDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select dm from DailyMvp dm where dm.mvpDate = :mvpDate")
    Optional<DailyMvp> findByMvpDateForUpdate(@Param("mvpDate") LocalDate mvpDate);

    @Query("select dm from DailyMvp dm where dm.mvpDate <= :targetDate and dm.rewardGranted = false")
    List<DailyMvp> findUnrewardedByDateLessThanEqual(@Param("targetDate") LocalDate targetDate);
}
