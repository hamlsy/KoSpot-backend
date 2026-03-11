package com.kospot.game.domain.event;

import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.member.domain.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoadViewPracticeEvent {
    private final Member member;
    private final RoadViewGame roadViewGame;
}
