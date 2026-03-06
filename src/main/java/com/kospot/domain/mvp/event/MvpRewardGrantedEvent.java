package com.kospot.domain.mvp.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class MvpRewardGrantedEvent {

    private final Long memberId;
    private final LocalDate mvpDate;
    private final Long roadViewGameId;
    private final int rewardPoint;
}
