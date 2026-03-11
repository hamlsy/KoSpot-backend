package com.kospot.multi.timer.application.message;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoundTransitionTimerMessage {

    private final Long nextRoundStartTimeMs; //다음 라운드 시작 시각
    private final Long serverTimestamp; //현재 서버 타임스탬프
    private final Boolean isLastRound; //마지막 라운드 여부

}
