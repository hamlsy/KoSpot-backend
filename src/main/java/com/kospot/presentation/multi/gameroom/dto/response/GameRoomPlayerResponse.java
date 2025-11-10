package com.kospot.presentation.multi.gameroom.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GameRoomPlayerResponse {

    private Long memberId;
    private String nickname;
    private boolean isHost;
    private String markerImageUrl;

}
