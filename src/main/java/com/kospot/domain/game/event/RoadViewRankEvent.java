package com.kospot.domain.game.event;

import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.gamerank.domain.entity.GameRank;
import com.kospot.member.domain.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoadViewRankEvent {
    private final Member member;
    private final RoadViewGame roadViewGame;
    private final GameRank gameRank;

}
