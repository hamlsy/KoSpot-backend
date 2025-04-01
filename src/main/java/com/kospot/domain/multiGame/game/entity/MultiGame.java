package com.kospot.domain.multiGame.game.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.entity.GameType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class MultiGame extends BaseTimeEntity {

    @Enumerated(EnumType.STRING)
    private GameType gameType;

}
