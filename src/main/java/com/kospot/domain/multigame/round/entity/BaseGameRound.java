package com.kospot.domain.multigame.round.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.infrastructure.exception.object.domain.GameRoundHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class BaseGameRound extends BaseTimeEntity {

    @Builder.Default
    private String roundId = UUID.randomUUID().toString();
    private Integer roundNumber;
    private Boolean isFinished = false;
    public Integer timeLimit; // in seconds

    private Instant serverStartTime; // 서버 시작 시간

    @Builder.Default
    private List<Long> playerIds = new ArrayList<>();

    public abstract GameMode getGameMode();

    //business methods
    public Duration getDuration() {
        if (timeLimit != null) {
            return Duration.ofSeconds(timeLimit);
        }
        return getGameMode().getDuration(); //기본값
    }

    public void startRound() {
        this.serverStartTime = Instant.now();
    }

    // 남은 시간
    public long getRemainingTimeMs() {
        if (this.serverStartTime == null) {
            return getDuration().toMillis();
        }
        Duration elapsed = Duration.between(this.serverStartTime, Instant.now());
        long remaining = getDuration().toMillis() - elapsed.toMillis();
        return Math.max(remaining, 0);
    }

    //타이머 종료 여부
    public boolean isTimeExpired() {
        return getRemainingTimeMs() <= 0;
    }

    public void finishRound() {
        validateRoundNotFinished();
        this.isFinished = true;
    }

    // validate
    public void validateRoundNotFinished() {
        if (this.isFinished) {
            throw new GameRoundHandler(ErrorStatus.ROUND_ALREADY_FINISHED);
        }
    }

}
