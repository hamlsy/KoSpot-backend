package com.kospot.domain.multi.submission.event;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class EarlyRoundCompletionEvent {

    private final String gameRoomId;
    private final Long gameId;
    private final Long roundId;
    private final GameMode gameMode;
    private final PlayerMatchType playerMatchType;

}
