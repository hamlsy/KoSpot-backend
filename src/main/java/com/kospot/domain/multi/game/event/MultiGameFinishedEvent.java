package com.kospot.domain.multi.game.event;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MultiGameFinishedEvent {
    
    private final Long gameId;
    private final GameMode gameMode;
    private final PlayerMatchType matchType;
    private final List<GamePlayer> gamePlayers;
    
    public static MultiGameFinishedEvent of(Long gameId, GameMode gameMode, 
                                            PlayerMatchType matchType, List<GamePlayer> gamePlayers) {
        return MultiGameFinishedEvent.builder()
                .gameId(gameId)
                .gameMode(gameMode)
                .matchType(matchType)
                .gamePlayers(gamePlayers)
                .build();
    }
}

