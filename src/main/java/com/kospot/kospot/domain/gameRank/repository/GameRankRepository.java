package com.kospot.kospot.domain.gameRank.repository;

import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRankRepository extends JpaRepository<GameRank, Long> {

    @Query("select r from GameRank r join fetch r.member where r.member = :member " +
            "and r.gameType = :gameType")
    GameRank findByMemberAndGameType(@Param("member") Member member, @Param("gameType") GameType gameType);

}
