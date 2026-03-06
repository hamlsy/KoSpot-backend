package com.kospot.multi.submission.entity.event;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.multi.game.domain.vo.PlayerMatchType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EarlyRoundCompletionEvent {

    private final String gameRoomId;
    private final Long gameId;
    private final Long roundId;
    private final GameMode gameMode;
    private final PlayerMatchType playerMatchType;

}
