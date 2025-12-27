package com.kospot.domain.gamerank.repository;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameRankRepository extends JpaRepository<GameRank, Long> {

    @Query("select r from GameRank r join fetch r.member where r.member = :member")
    List<GameRank> findAllByMember(@Param("member") Member member);

    @Query("select r from GameRank r join fetch r.member where r.member = :member " +
            "and r.gameMode = :gameMode")
    GameRank findByMemberAndGameMode(@Param("member") Member member, @Param("gameMode") GameMode gameMode);

    @Query("select count(gr) from GameRank gr where gr.gameMode = :gameMode")
    long countByGameMode(@Param("gameMode") GameMode gameMode);

    @Query("select count(gr) from GameRank gr where gr.gameMode = :gameMode " +
            "and gr.ratingScore > :ratingScore")
    long countByGameModeAndRatingScoreGreaterThan(
            @Param("gameMode") GameMode gameMode,
            @Param("ratingScore") int ratingScore
    );



}
