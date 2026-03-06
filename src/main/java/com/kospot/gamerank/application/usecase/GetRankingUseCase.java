package com.kospot.gamerank.application.usecase;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.gamerank.application.adaptor.GameRankAdaptor;
import com.kospot.gamerank.domain.entity.GameRank;
import com.kospot.gamerank.domain.vo.RankTier;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.gamerank.presentation.response.GameRankResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetRankingUseCase {

    private final MemberAdaptor memberAdaptor;
    private final GameRankAdaptor gameRankAdaptor;

    private final static int DEFAULT_SIZE = 20;

    public GameRankResponse.Ranking execute(
            Long memberId, String gameMode, String rankTier, int page) {
        Member member = memberAdaptor.queryById(memberId);

        GameMode mode = GameMode.fromKey(gameMode);
        RankTier tier = RankTier.fromKey(rankTier);
        Pageable pageable = Pageable.ofSize(DEFAULT_SIZE).withPage(page);
        GameRank myRank = gameRankAdaptor.queryByMemberAndGameMode(member, mode);
        Page<GameRank> gameRanks = gameRankAdaptor.queryPageByGameModeAndRankTierFetchMember(
                mode,
                tier,
                pageable
        );

        GameRankResponse.MyRankInfo myRankInfo = GameRankResponse.MyRankInfo.from(member, myRank);
        List<GameRankResponse.PlayerSummary> playerSummaries = gameRanks.map(
                gr -> GameRankResponse.PlayerSummary.from(gr.getMember(), gr)
        ).getContent();

        return GameRankResponse.Ranking.builder()
                .myRank(myRankInfo)
                .players(playerSummaries)
                .build();
    }

}
