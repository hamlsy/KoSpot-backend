package com.kospot.application.multi.submission.websocket.message;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerSubmissionMessage {

    private Long playerId;

}
