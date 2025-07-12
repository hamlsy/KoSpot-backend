package com.kospot.domain.multigame.gameResult.repository;

import com.kospot.domain.multigame.gameResult.entity.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {
}
