package com.kospot.domain.multi.submission.event;

import com.kospot.domain.game.vo.GameMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public class PlayerSubmissionCompletedEvent {

    private final String gameRoomId;
    private final GameMode mode;
    private final Long gameId;
    private final Long roundId;

}
