package com.kospot.domain.multi.game.event;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import lombok.Getter;

/**
 * 게임이 취소되었을 때 발행되는 도메인 이벤트
 * 로딩 타임아웃, 플레이어 이탈 등의 사유로 게임이 취소될 때 발행된다.
 */
@Getter
public class GameCancelledEvent {

    private final Long gameId;
    private final Long roomId;
    private final GameMode gameMode;
    private final PlayerMatchType matchType;
    private final CancellationReason reason;

    private GameCancelledEvent(Long gameId, Long roomId, GameMode gameMode,
                               PlayerMatchType matchType, CancellationReason reason) {
        this.gameId = gameId;
        this.roomId = roomId;
        this.gameMode = gameMode;
        this.matchType = matchType;
        this.reason = reason;
    }

    public static GameCancelledEvent of(Long gameId, Long roomId, GameMode gameMode,
                                        PlayerMatchType matchType, CancellationReason reason) {
        return new GameCancelledEvent(gameId, roomId, gameMode, matchType, reason);
    }

    public enum CancellationReason {
        LOADING_TIMEOUT,
        PLAYER_LEFT,
        HOST_LEFT,
        MANUAL_CANCEL
    }
}

