package com.kospot.kospot.domain.gameRank.repository;

import com.kospot.kospot.domain.gameRank.entity.GameRank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankRepository extends JpaRepository<GameRank, Long> {
}
