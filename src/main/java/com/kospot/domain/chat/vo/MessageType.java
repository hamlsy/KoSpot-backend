package com.kospot.domain.chat.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageType {

    LOBBY_CHAT("게임 방 채팅", "게임 방 채팅"),
    GAME_CHAT("게임 내 전체 채팅", "게임 진행 중 모든 플레이어 대상 채팅"),
    TEAM_CHAT("게임 내 팀 채팅", "게임 진행 중 같은 팀원들만 보는 채팅"),
    GLOBAL_CHAT("글로벌 로비 채팅", "게임 방 리스트 로비에서의 전체 채팅"),
    SYSTEM_CHAT("시스템 메시지", "입장/퇴장/게임 상태 변경 등 시스템 알림"),
    NOTICE_CHAT("공지사항", "게임 내 공지사항 메시지"),;

    private final String displayName;
    private final String description;

    public boolean isGameMessage() {
        return this == GAME_CHAT || this == TEAM_CHAT;
    }

    public boolean isChatMessage() {
        return this != SYSTEM_CHAT;
    }

}
