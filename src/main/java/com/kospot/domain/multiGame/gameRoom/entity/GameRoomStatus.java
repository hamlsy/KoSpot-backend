package com.kospot.domain.multiGame.gameRoom.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameRoomStatus {
    WAITING("대기"), PLAYING("진행중"),
    FINISHED("종료");

    private final String name;

}
