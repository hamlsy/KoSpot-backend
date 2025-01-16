package com.kospot.kospot.domain.pointHistory.repository;

import com.kospot.kospot.domain.pointHistory.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
}
