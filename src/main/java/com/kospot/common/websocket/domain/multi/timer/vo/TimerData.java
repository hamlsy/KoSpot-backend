package com.kospot.common.websocket.domain.multi.timer.vo;

import com.kospot.game.domain.vo.GameMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerData {

    private String roundId;
    private GameMode gameMode;
    private int durationSeconds;
    private long startTimeMillis;

}
