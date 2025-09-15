package com.kospot.domain.multigame.timer.vo;

import com.kospot.domain.game.vo.GameMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerData {

    private String gameId;
    private String roundId;
    private GameMode gameMode;
    private int durationSeconds;
    private long startTimeMillis;
    private List<String> playerIds;

}
