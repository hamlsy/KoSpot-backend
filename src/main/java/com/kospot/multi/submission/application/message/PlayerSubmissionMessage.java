package com.kospot.multi.submission.application.message;

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
