package com.kospot.application.rank.usecase;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gamerank.adaptor.GameRankAdaptor;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.rank.dto.response.GameRankResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetRankingUseCase {

    private final GameRankAdaptor gameRankAdaptor;

    private final static int DEFAULT_SIZE = 20;

    public GameRankResponse.Ranking execute(
            Member member, String gameMode, String rankTier, int page) {

        GameMode mode = GameMode.fromKey(gameMode);
        RankTier tier = RankTier.fromKey(rankTier);
        Pageable pageable = Pageable.ofSize(DEFAULT_SIZE).withPage(page);
//        GameRank
//        Page<GameRankResponse.Player>
    }

}
