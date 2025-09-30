package com.kospot.application.multiplayer.timer.message;

import com.kospot.domain.game.vo.GameMode;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimerStartMessage {

    private String roundId;
    private GameMode gameMode;
    private Long serverStartTimeMs;
    private Long durationMs;
    private Long serverTimestamp;

}
