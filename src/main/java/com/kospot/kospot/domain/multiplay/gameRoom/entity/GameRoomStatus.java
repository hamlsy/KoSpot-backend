package com.kospot.kospot.domain.multiplay.gameRoom.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameRoomStatus {
    WAITING("대기"), PLAYING("진행중"),
    FINISHED("종료");

    private final String name;

}
