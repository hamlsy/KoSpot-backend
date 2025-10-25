package com.kospot.application.multi.round.message;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameFinishedMessage {
    private Long gameId;
    private String message;
    private Long timestamp;
}
