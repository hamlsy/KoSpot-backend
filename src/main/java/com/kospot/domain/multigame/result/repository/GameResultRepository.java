package com.kospot.domain.multigame.result.repository;

import com.kospot.domain.multigame.result.entity.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {
}
