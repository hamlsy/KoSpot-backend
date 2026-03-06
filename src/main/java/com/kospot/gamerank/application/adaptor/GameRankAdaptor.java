package com.kospot.gamerank.application.adaptor;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.gamerank.domain.entity.GameRank;
import com.kospot.gamerank.infrastructure.persistence.GameRankRepository;
import com.kospot.gamerank.domain.vo.RankTier;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Adaptor
@RequiredArgsConstructor
public class GameRankAdaptor {

    private final GameRankRepository repository;

    public List<GameRank> queryAllByMember(Member member){
        return repository.findAllByMember(member);
    }

    public GameRank queryByMemberAndGameMode(Member member, GameMode gameMode) {
        return repository.findByMemberAndGameMode(member, gameMode);
    }

    public long queryTotalRankCountByGameMode(GameMode gameMode) {
        return repository.countByGameMode(gameMode);
    }

    public long queryHigherRankCountByGameModeAndRatingScore(GameMode gameMode, int ratingScore) {
        return repository.countByGameModeAndRatingScoreGreaterThan(gameMode, ratingScore);
    }

    public Page<GameRank> queryPageByGameModeAndRankTierFetchMember(
            GameMode gameMode,
            RankTier rankTier,
            Pageable pageable
    ) {
        // Implementation needed
        return repository.findPageByGameModeAndRankTierFetchMember(gameMode, rankTier, pageable);
    }

}
