package com.kospot.domain.multi.submission.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public class PlayerSubmissionCompletedEvent {

    private final String gameRoomId;
    private final Long gameId;
    private final Long roundId;
    private final Long playerId;
    private final Long currentSubmissionCount;

}
