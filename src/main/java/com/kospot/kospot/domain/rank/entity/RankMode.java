package com.kospot.kospot.domain.rank.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RankMode {
    ROAD_VIEW("ROADVIEW"), PHOTO("PHOTO");

    private final String key;

}
