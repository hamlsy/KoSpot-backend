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
    private final int totalRounds;
    private final int currentRound;
    private final long roundTimeLimit;
    private final Long roundId;
    private final long countdownMs;
    private final long deadlineTs;
    private final long issuedAt;
    private final List<GamePlayerResponse> players;
    private final Object payload;
}

