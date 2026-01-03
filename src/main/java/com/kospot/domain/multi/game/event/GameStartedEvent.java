package com.kospot.domain.multi.game.event;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import lombok.Getter;

import java.util.List;

/**
 * 게임이 시작되었을 때 발행되는 도메인 이벤트
 * 로딩 단계 초기화, 플레이어 알림 등의 후속 처리를 트리거한다.
 */
@Getter
public class GameStartedEvent {

    private final Long gameId;
    private final Long roomId;
    private final GameMode gameMode;
    private final PlayerMatchType matchType;
    private final List<Long> playerIds;
    private final int totalRounds;
    private final int timeLimit;

    private GameStartedEvent(Long gameId, Long roomId, GameMode gameMode,
                             PlayerMatchType matchType, List<Long> playerIds,
                             int totalRounds, int timeLimit) {
        this.gameId = gameId;
        this.roomId = roomId;
        this.gameMode = gameMode;
        this.matchType = matchType;
        this.playerIds = playerIds;
        this.totalRounds = totalRounds;
        this.timeLimit = timeLimit;
    }

    public static GameStartedEvent of(Long gameId, Long roomId, GameMode gameMode,
                                      PlayerMatchType matchType, List<Long> playerIds,
                                      int totalRounds, int timeLimit) {
        return new GameStartedEvent(gameId, roomId, gameMode, matchType,
                playerIds, totalRounds, timeLimit);
    }
}

