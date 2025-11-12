package com.kospot.presentation.multi.access.dto.response;

import com.kospot.presentation.multi.gameroom.dto.response.GameRoomDetailResponse;
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
    private GameRoomDetailResponse gameRoomDetailResponse;

    public static GameAccessResponse notAllowed(String message) {
        return GameAccessResponse.builder()
                .allowed(false)
                .message(message)
                .build();
    }

    public static GameAccessResponse allowed(GameRoomDetailResponse gameRoomDetailResponse) {
        return GameAccessResponse.builder()
                .allowed(true)
                .gameRoomDetailResponse(gameRoomDetailResponse)
                .build();
    }
}