package com.kospot.domain.multi.round.repository;

import com.kospot.domain.multi.round.entity.PhotoGameRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PhotoGameRoundRepository extends JpaRepository<PhotoGameRound, Long> {
    @Query("SELECT r FROM PhotoGameRound r WHERE r.multiPhotoGame.id = :gameId AND r.roundNumber = :roundNumber")
    Optional<PhotoGameRound> findByGameIdAndRoundNumber(
            @Param("gameId") Long gameId,
            @Param("roundNumber") Integer roundNumber);

    @Query("SELECT r FROM PhotoGameRound r WHERE r.multiPhotoGame.id = :gameId ORDER BY r.roundNumber ASC")
    List<PhotoGameRound> findAllByGameIdOrderByRoundNumber(@Param("gameId") Long gameId);

    @Query("SELECT r FROM PhotoGameRound r LEFT JOIN FETCH r.playerSubmissions WHERE r.id = :roundId")
    Optional<PhotoGameRound> findWithSubmissionsById(@Param("roundId") Long roundId);
}
