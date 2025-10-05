package com.kospot.infrastructure.websocket.domain.multi.timer.vo;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.round.entity.BaseGameRound;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

//todo apply
@Getter
@Builder
public class TimerStartCommand {

    private final String gameRoomId;
    private final BaseGameRound round;
    private final GameMode gameMode;
    private final PlayerMatchType matchType;
    private final Long gameId;
    private final List<Long> playerIds;

}
