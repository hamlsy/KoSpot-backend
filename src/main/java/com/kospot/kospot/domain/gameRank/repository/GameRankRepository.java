package com.kospot.kospot.domain.gameRank.repository;

import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GameRankRepository extends JpaRepository<GameRank, Long> {

    @Query("select r from GameRank join fetch r.member.id where member.id = :memberId " +
            "and gameType = :gameType")
    GameRank findByMemberIdAndGameType(Long memberId, GameType gameType);

}
