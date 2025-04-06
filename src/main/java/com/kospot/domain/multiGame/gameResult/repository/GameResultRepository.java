package com.kospot.domain.multiGame.gameResult.repository;

import com.kospot.domain.multiGame.gameResult.entity.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {
}
