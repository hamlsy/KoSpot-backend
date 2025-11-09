package com.kospot.presentation.multi.flow.dto.message;

import com.kospot.presentation.multi.gamePlayer.dto.response.GamePlayerResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RoomGameStartMessage {
    private final String target;
    private final String gameMode;
    private final String matchType;
    private final Long gameId;
    private final Long roundId;
    private final int totalRounds;
    private final long roundTimeLimit;
    private final long issuedAt;
    private final List<GamePlayerResponse> players;
    private final Object payload;
}

