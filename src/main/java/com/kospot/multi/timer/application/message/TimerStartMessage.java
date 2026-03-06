package com.kospot.multi.timer.application.message;

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
