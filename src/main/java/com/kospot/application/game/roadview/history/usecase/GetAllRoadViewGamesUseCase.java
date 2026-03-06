package com.kospot.application.game.roadview.history.usecase;

import com.kospot.domain.game.adaptor.RoadViewGameAdaptor;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.presentation.game.dto.response.RoadViewGameHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAllRoadViewGamesUseCase {

    private final MemberAdaptor memberAdaptor;
    private final RoadViewGameAdaptor roadViewGameAdaptor;

    public RoadViewGameHistoryResponse.All execute(Long memberId, Pageable pageable) {
        Member member = memberAdaptor.queryById(memberId);
        Page<RoadViewGame> games = roadViewGameAdaptor.queryAllGamesByMember(member, pageable);
        return RoadViewGameHistoryResponse.All.from(games);
    }
}

