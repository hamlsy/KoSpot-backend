package com.kospot.multi.round.infrastructure.persistence;

import com.kospot.multi.round.entity.RoadViewGameRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;

import java.util.List;
import java.util.Optional;

/**
 * 로드뷰 게임 라운드 Repository
 */
public interface RoadViewGameRoundRepository extends JpaRepository<RoadViewGameRound, Long> {
    
    List<RoadViewGameRound> findAllByMultiRoadViewGameId(Long gameId);

    @Query("SELECT rvr FROM RoadViewGameRound rvr " +
           "JOIN FETCH rvr.targetCoordinate " +
           "WHERE rvr.id = :id")
    Optional<RoadViewGameRound> findByIdFetchCoordinate(@Param("id") Long id);

    @Query("SELECT rvr FROM RoadViewGameRound rvr " +
            "JOIN FETCH rvr.multiRoadViewGame " +
            "WHERE rvr.id = :id")
    Optional<RoadViewGameRound> findByIdFetchGame(@Param("id") Long id);

    @Query("SELECT rvr FROM RoadViewGameRound rvr " +
            "JOIN FETCH rvr.targetCoordinate " +
            "JOIN FETCH rvr.multiRoadViewGame " +
            "WHERE rvr.id = :id")
    Optional<RoadViewGameRound> findByIdFetchCoordinateAndGame(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rvr FROM RoadViewGameRound rvr " +
            "JOIN FETCH rvr.targetCoordinate " +
            "JOIN FETCH rvr.multiRoadViewGame " +
            "WHERE rvr.id = :id")
    Optional<RoadViewGameRound> findByIdFetchGameForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RoadViewGameRound rvr " +
            "SET rvr.roundVersion = rvr.roundVersion + 1, " +
            "    rvr.reissueCount = rvr.reissueCount + 1, " +
            "    rvr.lastReissueAt = :now " +
            "WHERE rvr.id = :roundId " +
            "  AND rvr.multiRoadViewGame.id = :gameId " +
            "  AND rvr.roundVersion = :expectedVersion " +
            "  AND rvr.isFinished = false " +
            "  AND rvr.serverStartTime IS NULL " +
            "  AND rvr.reissueCount < :maxReissueCount " +
            "  AND (rvr.lastReissueAt IS NULL OR rvr.lastReissueAt <= :cooldownThreshold)")
    int tryAdvanceReissueVersion(@Param("roundId") Long roundId,
                                 @Param("gameId") Long gameId,
                                 @Param("expectedVersion") Long expectedVersion,
                                 @Param("maxReissueCount") Integer maxReissueCount,
                                 @Param("cooldownThreshold") Instant cooldownThreshold,
                                 @Param("now") Instant now);
}
