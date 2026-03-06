package com.kospot.multi.round.application.message;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameFinishedMessage {
    private Long gameId;
    private String message;
    private Long timestamp;
}
