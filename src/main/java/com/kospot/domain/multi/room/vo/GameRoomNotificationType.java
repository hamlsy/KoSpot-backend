package com.kospot.domain.multi.room.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameRoomNotificationType {
    PLAYER_JOINED("플레이어 입장"),
    PLAYER_LEFT("플레이어 퇴장"),
    PLAYER_KICKED("플레이어 강퇴"),
    ROOM_SETTINGS_CHANGED("방 설정 변경"),
    PLAYER_LIST_UPDATED("플레이어 목록 갱신"),
    GAME_STARTED("게임 시작"),
    HOST_CHANGED("방장 변경");

    private final String description;

}
