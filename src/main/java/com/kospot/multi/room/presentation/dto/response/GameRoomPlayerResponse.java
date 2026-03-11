package com.kospot.multi.room.presentation.dto.response;

import com.kospot.multi.room.domain.vo.MultiplayerScreenState;
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
    private MultiplayerScreenState screenState;

}
