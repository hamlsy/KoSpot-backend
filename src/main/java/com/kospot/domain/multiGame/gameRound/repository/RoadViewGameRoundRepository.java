package com.kospot.domain.multiGame.gameRound.repository;

import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoadViewGameRoundRepository extends JpaRepository<RoadViewGameRound, Long> {
    List<RoadViewGameRound> findAllByMultiRoadViewGameId(Long gameId);
}
