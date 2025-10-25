package com.kospot.domain.multi.timer.event;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoundCompletionEvent {

    private final String gameRoomId;
    private final Long gameId;
    private final Long roundId;
    private final GameMode gameMode;
    private final PlayerMatchType playerMatchType;

}
