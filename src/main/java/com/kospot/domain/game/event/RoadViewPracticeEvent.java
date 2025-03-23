package com.kospot.domain.game.event;

import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoadViewPracticeEvent {
    private final Member member;
    private final RoadViewGame roadViewGame;
}
