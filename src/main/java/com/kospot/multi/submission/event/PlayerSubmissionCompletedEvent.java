package com.kospot.multi.submission.event;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.multi.game.domain.vo.PlayerMatchType;
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
