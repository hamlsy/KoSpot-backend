package com.kospot.presentation.multi.access.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameAccessResponse {
    private boolean allowed;              // 접근 가능 여부
    private String message;                // 접근 불가 시 메시지
//    private GameState gameState;          // 게임 상태 (WAITING, IN_PROGRESS, FINISHED)
    private GameInfo gameInfo;             // 게임 정보 (접근 가능한 경우)

    @Data
    @Builder
    public static class GameInfo {
        private Long gameId;
        private String roomId;
//        private List<PlayerInfo> players;  // 플레이어 정보 포함
//        private GameSettings settings;
    }
}