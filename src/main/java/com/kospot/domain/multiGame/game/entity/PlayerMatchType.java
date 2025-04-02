package com.kospot.domain.multiGame.game.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlayerMatchType {
    INDIVIDUAL("개인전"),
    COOPERATIVE("협동전");

    private final String description;
} 