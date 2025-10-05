package com.kospot.domain.multi.result.repository;

import com.kospot.domain.multi.result.entity.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {
}
