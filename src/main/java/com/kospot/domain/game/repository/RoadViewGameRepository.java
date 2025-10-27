package com.kospot.domain.game.repository;

import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.vo.GameStatus;
import com.kospot.domain.game.vo.GameType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoadViewGameRepository extends JpaRepository<RoadViewGame, Long> {

    @Query("select rg from RoadViewGame rg where rg.member.id = :memberId")
    List<RoadViewGame> findByMemberId(@Param("memberId") Long memberId);

    @Query("select rg from RoadViewGame rg where rg.member.email = :memberEmail")
    List<RoadViewGame> findByMemberEmail(@Param("memberEmail") String memberEmail);

    @Query("select rg from RoadViewGame rg join fetch rg.coordinate where rg.id = :id")
    Optional<RoadViewGame> findByIdFetchCoordinate(@Param("id") Long id);

    @Query("select count(rg) from RoadViewGame rg where rg.member.id = :memberId and rg.gameType = :gameType and rg.gameStatus = :gameStatus")
    long countByMemberIdAndGameTypeAndGameStatus(@Param("memberId") Long memberId, @Param("gameType") GameType gameType, @Param("gameStatus") GameStatus gameStatus);

    @Query("select coalesce(avg(rg.score), 0.0) from RoadViewGame rg where rg.member.id = :memberId and rg.gameType = :gameType and rg.gameStatus = :gameStatus")
    double findAverageScoreByMemberIdAndGameType(@Param("memberId") Long memberId, @Param("gameType") GameType gameType, @Param("gameStatus") GameStatus gameStatus);

    @Query("select coalesce(max(rg.score), 0.0) from RoadViewGame rg where rg.member.id = :memberId and rg.gameStatus = :gameStatus")
    double findMaxScoreByMemberId(@Param("memberId") Long memberId, @Param("gameStatus") GameStatus gameStatus);

    @Query("select rg.createdDate from RoadViewGame rg where rg.member.id = :memberId order by rg.createdDate desc")
    List<java.time.LocalDateTime> findAllCreatedDatesByMemberId(@Param("memberId") Long memberId);

    @Query("select rg.createdDate from RoadViewGame rg where rg.member.id = :memberId and rg.gameStatus = :gameStatus order by rg.createdDate desc limit 1")
    java.time.LocalDateTime findLatestPlayDateByMemberId(@Param("memberId") Long memberId, @Param("gameStatus") GameStatus gameStatus);
}
