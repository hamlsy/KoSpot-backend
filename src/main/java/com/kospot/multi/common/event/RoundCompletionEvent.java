package com.kospot.multi.common.event;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.multi.game.domain.vo.PlayerMatchType;
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
