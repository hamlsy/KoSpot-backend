package com.kospot.application.multi.timer.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimerSyncMessage {

    private String roundId;
    private long remainingTimeMs;
    private long serverTimestamp;
    private boolean isFinalCountDown;

}
