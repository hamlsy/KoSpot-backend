package com.kospot.infrastructure.redis.domain.multi.timer.event;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public class RoundCompletionEvent {

    private final String gameRoomId;
    private final String roundId;
    private final GameMode gameMode;
    private final PlayerMatchType playerMatchType;
    private final String gameId;

}
