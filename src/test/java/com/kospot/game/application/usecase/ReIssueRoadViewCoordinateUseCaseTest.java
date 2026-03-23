package com.kospot.game.application.usecase;

import com.kospot.common.exception.object.domain.GameHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.coordinate.application.adaptor.CoordinateAdaptor;
import com.kospot.coordinate.application.service.CoordinateService;
import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.application.service.AESService;
import com.kospot.game.application.service.RoadViewGameService;
import com.kospot.game.application.usecase.rank.ReIssueRoadViewCoordinateUseCase;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.infrastructure.redis.adaptor.AnonymousPracticeTokenRedisAdaptor;
import com.kospot.game.presentation.dto.response.StartGameResponse;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReIssueRoadViewCoordinateUseCase 단위 테스트")
class ReIssueRoadViewCoordinateUseCaseTest {

    @Mock private MemberAdaptor memberAdaptor;
    @Mock private RoadViewGameAdaptor roadViewGameAdaptor;
    @Mock private RoadViewGameService roadViewGameService;
    @Mock private CoordinateService coordinateService;
    @Mock private CoordinateAdaptor coordinateAdaptor;
    @Mock private AESService aesService;
    @Mock private AnonymousPracticeTokenRedisAdaptor anonymousPracticeTokenRedisAdaptor;

    @InjectMocks
    private ReIssueRoadViewCoordinateUseCase useCase;

    private Member owner;
    private Member other;
    private RoadViewGame game;
    private Coordinate oldCoordinate;
    private Coordinate newCoordinate;

    @BeforeEach
    void setUp() {
        owner = Member.builder().id(1L).nickname("owner").build();
        other = Member.builder().id(2L).nickname("other").build();

        oldCoordinate = mock(Coordinate.class);
        newCoordinate = mock(Coordinate.class);
        when(newCoordinate.getPoiName()).thenReturn("부산 해운대");

        game = mock(RoadViewGame.class);
        when(game.getId()).thenReturn(10L);
        when(game.getMember()).thenReturn(owner);
        when(game.getCoordinate()).thenReturn(newCoordinate);
        when(game.getGameType()).thenReturn(com.kospot.game.domain.vo.GameType.PRACTICE);
        when(game.getPracticeSido()).thenReturn(null);

        when(roadViewGameAdaptor.queryByIdFetchCoordinate(10L)).thenReturn(game);
        when(coordinateAdaptor.getRandomCoordinate()).thenReturn(newCoordinate);
        when(aesService.toEncryptString(any())).thenReturn("encrypted");
    }

    @Test
    @DisplayName("로그인 소유자 - 좌표 재발급 성공")
    void execute_loggedInOwner_reissuesCoordinate() {
        when(memberAdaptor.queryById(1L)).thenReturn(owner);
        // game.getMember().getId() == owner.getId() → ownership OK

        StartGameResponse.ReIssue response = useCase.execute(1L, 10L, null);

        verify(coordinateService).invalidateCoordinate(any());
        verify(roadViewGameService).updateCoordinate(game, newCoordinate);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("로그인 비소유자 - GAME_NOT_SAME_MEMBER 예외")
    void execute_loggedInNonOwner_throwsException() {
        when(memberAdaptor.queryById(2L)).thenReturn(other);
        // game.getMember().getId() == 1L, other.getId() == 2L → mismatch

        assertThatThrownBy(() -> useCase.execute(2L, 10L, null))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.GAME_NOT_SAME_MEMBER));

        verify(coordinateService, never()).invalidateCoordinate(any());
    }

    @Test
    @DisplayName("게임의 member가 null인 경우 - GAME_NOT_SAME_MEMBER 예외")
    void execute_gameHasNullMember_throwsException() {
        when(game.getMember()).thenReturn(null);
        when(memberAdaptor.queryById(1L)).thenReturn(owner);

        assertThatThrownBy(() -> useCase.execute(1L, 10L, null))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.GAME_NOT_SAME_MEMBER));
    }

    @Test
    @DisplayName("비로그인 사용자 - 토큰 검증 후 좌표 재발급 성공")
    void execute_anonymous_validatesTokenAndReissues() {
        useCase.execute(null, 10L, "valid-token");

        verify(anonymousPracticeTokenRedisAdaptor).validate(10L, "valid-token");
        verify(coordinateService).invalidateCoordinate(any());
        verify(roadViewGameService).updateCoordinate(game, newCoordinate);
        verify(memberAdaptor, never()).queryById(anyLong());
    }

    @Test
    @DisplayName("비로그인 사용자 - 토큰 없으면 validate에서 예외 전파")
    void execute_anonymous_nullToken_propagatesException() {
        doThrow(new GameHandler(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_REQUIRED))
                .when(anonymousPracticeTokenRedisAdaptor).validate(10L, null);

        assertThatThrownBy(() -> useCase.execute(null, 10L, null))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_REQUIRED));

        verify(coordinateService, never()).invalidateCoordinate(any());
    }
}
