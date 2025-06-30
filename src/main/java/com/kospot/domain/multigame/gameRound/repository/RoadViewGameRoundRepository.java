package com.kospot.domain.multigame.gameRound.repository;

import com.kospot.domain.multigame.gameRound.entity.RoadViewGameRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoadViewGameRoundRepository extends JpaRepository<RoadViewGameRound, Long> {
    List<RoadViewGameRound> findAllByMultiRoadViewGameId(Long gameId);

    @Query("select r from RoadViewGameRound r " +
            "join fetch r.roadViewPlayerSubmissions rps " +
            "left join fetch rps.gamePlayer gp " +
            "where r.id = :id")
    Optional<RoadViewGameRound> findByIdFetchPlayerSubmissionAndPlayers(@Param("id") Long id);

}
