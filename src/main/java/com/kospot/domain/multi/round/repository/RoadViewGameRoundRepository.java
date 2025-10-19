package com.kospot.domain.multi.round.repository;

import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 로드뷰 게임 라운드 Repository
 */
public interface RoadViewGameRoundRepository extends JpaRepository<RoadViewGameRound, Long> {
    
    List<RoadViewGameRound> findAllByMultiRoadViewGameId(Long gameId);

}
