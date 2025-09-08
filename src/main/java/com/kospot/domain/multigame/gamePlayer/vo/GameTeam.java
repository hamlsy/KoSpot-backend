package com.kospot.domain.multigame.gamePlayer.vo;

import com.kospot.domain.multigame.gamePlayer.exception.GameTeamErrorStatus;
import com.kospot.domain.multigame.gamePlayer.exception.GameTeamHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum GameTeam {
    RED, BLUE, GREEN, YELLOW;

    public static GameTeam fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new GameTeamHandler(GameTeamErrorStatus.GAME_TEAM_NOT_FOUND);
        }
        return Stream.of(GameTeam.values())
                .filter(team -> team.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(
                        () -> new GameTeamHandler(GameTeamErrorStatus.GAME_TEAM_NOT_FOUND)
                );
    }
}
