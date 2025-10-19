package com.kospot.application.multi.submission.websocket.message;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PlayerSubmissionMessage {

    private Long playerId;
    private Long roundId;
    private Instant timestamp;

}
