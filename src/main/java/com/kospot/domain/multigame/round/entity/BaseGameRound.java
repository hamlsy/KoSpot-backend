package com.kospot.domain.multigame.round.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.infrastructure.exception.object.domain.GameRoundHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class BaseGameRound extends BaseTimeEntity {

    private Integer roundNumber;
    private Boolean isFinished = false;
    public Duration duration;

    @Builder.Default
    private List<Long> playerIds = new ArrayList<>();

    public abstract GameMode getGameMode();

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
