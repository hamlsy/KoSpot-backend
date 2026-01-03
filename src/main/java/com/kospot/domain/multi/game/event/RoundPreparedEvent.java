package com.kospot.domain.multi.game.event;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import lombok.Getter;

/**
 * 라운드 준비가 완료되었을 때 발행되는 도메인 이벤트
 * 라운드 시작 브로드캐스트, 타이머 시작 등의 후속 처리를 트리거한다.
 */
@Getter
public class RoundPreparedEvent {

    private final Long gameId;
    private final Long roomId;
    private final Long roundId;
    private final int roundNumber;
    private final GameMode gameMode;
    private final PlayerMatchType matchType;
    private final boolean isInitialRound;

    private RoundPreparedEvent(Long gameId, Long roomId, Long roundId, int roundNumber,
                               GameMode gameMode, PlayerMatchType matchType, boolean isInitialRound) {
        this.gameId = gameId;
        this.roomId = roomId;
        this.roundId = roundId;
        this.roundNumber = roundNumber;
        this.gameMode = gameMode;
        this.matchType = matchType;
        this.isInitialRound = isInitialRound;
    }

    public static RoundPreparedEvent of(Long gameId, Long roomId, Long roundId, int roundNumber,
                                        GameMode gameMode, PlayerMatchType matchType, boolean isInitialRound) {
        return new RoundPreparedEvent(gameId, roomId, roundId, roundNumber,
                gameMode, matchType, isInitialRound);
    }

    public static RoundPreparedEvent initial(Long gameId, Long roomId, Long roundId, int roundNumber,
                                             GameMode gameMode, PlayerMatchType matchType) {
        return new RoundPreparedEvent(gameId, roomId, roundId, roundNumber,
                gameMode, matchType, true);
    }

    public static RoundPreparedEvent next(Long gameId, Long roomId, Long roundId, int roundNumber,
                                          GameMode gameMode, PlayerMatchType matchType) {
        return new RoundPreparedEvent(gameId, roomId, roundId, roundNumber,
                gameMode, matchType, false);
    }
}

