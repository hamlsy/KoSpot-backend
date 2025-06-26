package com.kospot.domain.message.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChannelType {
    GLOBAL_LOBBY("글로벌 로비"),
    GAME_ROOM("게임 방 대기실"),
    IN_GAME("게임 내부");

    private final String description;

}
