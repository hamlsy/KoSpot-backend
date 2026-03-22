package com.kospot.game.application.usecase;

import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.application.service.RoadViewGameService;
import com.kospot.game.application.usecase.practice.usecase.EndRoadViewPracticeUseCase;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.infrastructure.redis.adaptor.AnonymousPracticeTokenRedisAdaptor;
import com.kospot.game.presentation.dto.request.EndGameRequest;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EndRoadViewPracticeUseCase 단위 테스트")
class EndRoadViewPracticeUseCaseTest {

    @Mock private MemberAdaptor memberAdaptor;
    @Mock private RoadViewGameAdaptor roadViewGameAdaptor;
    @Mock private RoadViewGameService roadViewGameService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AnonymousPracticeTokenRedisAdaptor anonymousPracticeTokenRedisAdaptor;

    @InjectMocks
    private EndRoadViewPracticeUseCase useCase;

    private Member member;
    private RoadViewGame game;
    private EndGameRequest.RoadView request;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).nickname("player").build();
        Coordinate coordinate = mock(Coordinate.class);
        game = mock(RoadViewGame.class);
        when(game.getId()).thenReturn(10L);
        when(game.getCoordinate()).thenReturn(coordinate);
        when(game.getScore()).thenReturn(800.0);

        request = EndGameRequest.RoadView.builder().gameId(10L).build();
        when(roadViewGameAdaptor.queryByIdFetchCoordinate(10L)).thenReturn(game);
    }

    @Test
    @DisplayName("로그인 사용자 - finishGame 호출 및 이벤트 발행")
    void execute_loggedIn_finishesGameAndPublishesEvent() {
        when(memberAdaptor.queryById(1L)).thenReturn(member);

        useCase.execute(1L, request, null);

        verify(roadViewGameService).finishGame(member, game, request);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("비로그인 사용자 - 토큰 검증, finishGameAnonymous 호출, 토큰 삭제, 이벤트 미발행")
    void execute_anonymous_finishesAnonymousGameWithoutEvent() {
        useCase.execute(null, request, "valid-token");

        verify(anonymousPracticeTokenRedisAdaptor).validate(10L, "valid-token");
        verify(roadViewGameService).finishGameAnonymous(game, request);
        verify(anonymousPracticeTokenRedisAdaptor).delete(10L);
        verify(eventPublisher, never()).publishEvent(any());
        verify(memberAdaptor, never()).queryById(anyLong());
    }

    @Test
    @DisplayName("비로그인 사용자 - 토큰 검증 순서 확인 (validate → finishGameAnonymous → delete)")
    void execute_anonymous_callOrderIsCorrect() {
        var inOrder = inOrder(anonymousPracticeTokenRedisAdaptor, roadViewGameService);

        useCase.execute(null, request, "token");

        inOrder.verify(anonymousPracticeTokenRedisAdaptor).validate(10L, "token");
        inOrder.verify(roadViewGameService).finishGameAnonymous(game, request);
        inOrder.verify(anonymousPracticeTokenRedisAdaptor).delete(10L);
    }
}
