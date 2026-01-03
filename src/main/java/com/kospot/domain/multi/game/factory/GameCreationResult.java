package com.kospot.domain.multi.game.factory;

import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import lombok.Getter;

import java.util.List;

/**
 * 게임 생성 결과를 담는 Value Object
 * 생성된 게임과 플레이어 목록을 함께 반환한다.
 */
@Getter
public class GameCreationResult {

    private final MultiRoadViewGame game;
    private final List<GamePlayer> players;

    private GameCreationResult(MultiRoadViewGame game, List<GamePlayer> players) {
        this.game = game;
        this.players = List.copyOf(players);
    }

    public static GameCreationResult of(MultiRoadViewGame game, List<GamePlayer> players) {
        return new GameCreationResult(game, players);
    }

    public Long getGameId() {
        return game.getId();
    }

    public List<Long> getPlayerIds() {
        return players.stream()
                .map(GamePlayer::getId)
                .toList();
    }

    public List<Long> getMemberIds() {
        return players.stream()
                .map(player -> player.getMember().getId())
                .toList();
    }

    public int getPlayerCount() {
        return players.size();
    }
}

