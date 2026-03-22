package com.kospot.game.domain;

import com.kospot.common.exception.object.domain.GameHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.domain.vo.GameMode;
import com.kospot.game.domain.vo.GameStatus;
import com.kospot.game.domain.vo.GameType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RoadViewGame.endAnonymous() 도메인 단위 테스트")
class GameAnonymousEndTest {

    @Test
    @DisplayName("익명 게임 종료 - 정상 동작")
    void endAnonymous_success() {
        RoadViewGame game = RoadViewGame.builder()
                .gameMode(GameMode.ROADVIEW)
                .gameType(GameType.PRACTICE)
                .gameStatus(GameStatus.ABANDONED)
                .build();

        game.endAnonymous(37.5, 127.0, 30_000.0, 500.0);

        assertThat(game.getGameStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(game.getAnswerDistance()).isEqualTo(500.0);
        assertThat(game.getScore()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("이미 완료된 게임에 endAnonymous 호출 시 예외 발생")
    void endAnonymous_alreadyCompleted_throws() {
        RoadViewGame game = RoadViewGame.builder()
                .gameMode(GameMode.ROADVIEW)
                .gameType(GameType.PRACTICE)
                .gameStatus(GameStatus.COMPLETED)
                .build();

        assertThatThrownBy(() -> game.endAnonymous(37.5, 127.0, 30_000.0, 500.0))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> {
                    GameHandler handler = (GameHandler) ex;
                    assertThat(handler.getCode()).isEqualTo(ErrorStatus.GAME_IS_ALREADY_COMPLETED);
                });
    }
}
