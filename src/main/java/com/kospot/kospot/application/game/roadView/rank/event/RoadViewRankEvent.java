package com.kospot.kospot.application.game.roadView.rank.event;

import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public class RoadViewRankEvent {
    private final Member member;
    private final RoadViewGame roadViewGame;
}
