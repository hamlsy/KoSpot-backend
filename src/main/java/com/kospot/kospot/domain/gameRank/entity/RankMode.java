package com.kospot.kospot.domain.gameRank.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RankMode {
    ROAD_VIEW("ROADVIEW"), PHOTO("PHOTO");

    private final String key;

}
