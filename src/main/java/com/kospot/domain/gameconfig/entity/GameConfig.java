package com.kospot.domain.gameconfig.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game_config",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"game_mode", "player_match_type", "is_single_mode"})
        }
)
public class GameConfig extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_config_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameMode gameMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "player_match_type")
    private PlayerMatchType playerMatchType; // 멀티플레이 전용 (SOLO, TEAM)

    @Column(nullable = false)
    private Boolean isSingleMode; // true: 싱글 모드, false: 멀티 모드

    @Column(nullable = false)
    private Boolean isActive;

    // Business Logic
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public boolean isSingle() {
        return this.isSingleMode;
    }

    public boolean isMulti() {
        return !this.isSingleMode;
    }
}

