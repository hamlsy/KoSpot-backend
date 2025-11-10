package com.kospot.application.multi.timer.message;

import com.kospot.domain.game.vo.GameMode;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimerStartMessage {

    private String roundId;
    private String gameMode;
    private Long serverStartTimeMs;
    private Long durationMs;
    private Long serverTimestamp;

}
