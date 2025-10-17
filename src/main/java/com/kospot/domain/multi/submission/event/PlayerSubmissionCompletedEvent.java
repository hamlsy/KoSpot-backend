package com.kospot.domain.multi.submission.event;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlayerSubmissionCompletedEvent {

    private final String gameRoomId;
    private final Long gameId;
    private final Long roundId;
    private final GameMode mode;
    private final PlayerMatchType matchType;

}
