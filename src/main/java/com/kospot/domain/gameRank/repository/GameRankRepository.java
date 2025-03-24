package com.kospot.domain.gameRank.repository;

import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.gameRank.entity.GameRank;
import com.kospot.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRankRepository extends JpaRepository<GameRank, Long> {

    @Query("select r from GameRank r join fetch r.member where r.member = :member " +
            "and r.gameMode = :gameMode")
    GameRank findByMemberAndGameMode(@Param("member") Member member, @Param("gameMode") GameMode gameMode);

}
