package com.kospot.domain.point.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointHistoryType {

    ITEM_PURCHASE("아이템 구매"),
    PRACTICE_GAME("연습게임 플레이"),
    RANK_GAME("랭크게임 플레이"),
    MULTI_GAME("멀티게임 플레이"),
    DAILY_BONUS("출석 보너스"),
    EVENT_REWARD("이벤트 보상"),;

    private final String description;

}
