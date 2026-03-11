package com.kospot.multi.timer.entity.vo;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.multi.game.domain.vo.PlayerMatchType;
import com.kospot.multi.round.entity.BaseGameRound;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimerCommand {

    private final String gameRoomId;
    private final BaseGameRound round;
    private final GameMode gameMode;
    private final PlayerMatchType matchType;
    private final Long gameId;

}
