package com.kospot.kospot.domain.point.repository;

import com.kospot.kospot.domain.game.entity.GameMode;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    @Query("select p from PointHistory p join fetch p.member.id where p.member = :memberId")
    List<PointHistory> findAllByMemberId(Long memberId);

    // todo refactor
    @Query("select p from PointHistory p join fetch where p.member.id = :memberId and p.gameType = :gameType")
    List<PointHistory> findByMemberIdAndGameType(Long memberId, GameType gameType);

    // todo refactor
    @Query("select p from PointHistory p join fetch where p.member.id = :memberId " +
            "and p.gameType = :gameType " +
            "and p.gameMode = :gameMode")
    List<PointHistory> findByMemberIdAndGameTypeAndGameMode(Long memberId, GameType gameType, GameMode gameMode);

}
