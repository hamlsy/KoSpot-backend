package com.kospot.game.application.usecase;

import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.game.application.service.AESService;
import com.kospot.game.application.service.RoadViewGameService;
import com.kospot.game.application.usecase.practice.usecase.StartRoadViewPracticeUseCase;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.infrastructure.redis.adaptor.AnonymousPracticeTokenRedisAdaptor;
import com.kospot.game.presentation.dto.response.StartGameResponse;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StartRoadViewPracticeUseCase 단위 테스트")
class StartRoadViewPracticeUseCaseTest {

    @Mock private MemberAdaptor memberAdaptor;
    @Mock private RoadViewGameService roadViewGameService;
    @Mock private AESService aesService;
    @Mock private MemberProfileRedisAdaptor memberProfileRedisAdaptor;
    @Mock private AnonymousPracticeTokenRedisAdaptor anonymousPracticeTokenRedisAdaptor;

    @InjectMocks
    private StartRoadViewPracticeUseCase useCase;

    private Member member;
    private RoadViewGame game;
    private Coordinate coordinate;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).nickname("player").build();
        coordinate = mock(Coordinate.class);
        when(coordinate.getPoiName()).thenReturn("서울 광화문");

        game = mock(RoadViewGame.class);
        when(game.getId()).thenReturn(10L);
        when(game.getCoordinate()).thenReturn(coordinate);
        when(aesService.toEncryptString(any())).thenReturn("encrypted");
    }

    @Test
    @DisplayName("로그인 사용자 - startPracticeGame 호출, practiceToken 없음")
    void execute_loggedIn_startsGameWithMember() {
        MemberProfileRedisAdaptor.MemberProfileView profile =
                new MemberProfileRedisAdaptor.MemberProfileView(1L, "player", "http://img.url");
        when(memberAdaptor.queryById(1L)).thenReturn(member);
        when(roadViewGameService.startPracticeGame(member, "SEOUL")).thenReturn(game);
        when(memberProfileRedisAdaptor.findProfile(1L)).thenReturn(profile);

        StartGameResponse.RoadView response = useCase.execute(1L, "SEOUL");

        assertThat(response.getGameId()).isEqualTo(10L);
        assertThat(response.getPracticeToken()).isNull();
        assertThat(response.getMarkerImageUrl()).isEqualTo("http://img.url");
        verify(roadViewGameService).startPracticeGame(member, "SEOUL");
        verify(anonymousPracticeTokenRedisAdaptor, never()).generateAndStore(anyLong());
    }

    @Test
    @DisplayName("비로그인 사용자 - startAnonymousPracticeGame 호출, practiceToken 반환")
    void execute_anonymous_startsAnonymousGameWithToken() {
        when(roadViewGameService.startAnonymousPracticeGame("SEOUL")).thenReturn(game);
        when(anonymousPracticeTokenRedisAdaptor.generateAndStore(10L)).thenReturn("uuid-token");

        StartGameResponse.RoadView response = useCase.execute(null, "SEOUL");

        assertThat(response.getGameId()).isEqualTo(10L);
        assertThat(response.getPracticeToken()).isEqualTo("uuid-token");
        assertThat(response.getMarkerImageUrl()).isNull();
        verify(roadViewGameService).startAnonymousPracticeGame("SEOUL");
        verify(memberAdaptor, never()).queryById(anyLong());
    }
}
