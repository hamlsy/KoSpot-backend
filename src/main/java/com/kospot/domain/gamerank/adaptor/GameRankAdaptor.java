package com.kospot.domain.gamerank.adaptor;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.repository.GameRankRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class GameRankAdaptor {

    private final GameRankRepository repository;

    public GameRank queryByMemberAndGameMode(Member member, GameMode gameMode) {
        return repository.findByMemberAndGameMode(member, gameMode);
    }

    public long queryTotalRankCountByGameMode(GameMode gameMode) {
        return repository.countByGameMode(gameMode);
    }

    public long queryHigherRankCountByGameModeAndRatingScore(GameMode gameMode, int ratingScore) {
        return repository.countByGameModeAndRatingScoreGreaterThan(gameMode, ratingScore);
    }

}
