package com.kospot.multiGame.submission.usecase.roadview;

import com.kospot.application.multiGame.submission.SubmitRoadViewPlayerAnswerUseCase;
import com.kospot.domain.multiGame.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gameRound.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multiGame.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.domain.multiGame.submission.service.RoadViewPlayerSubmissionService;
import com.kospot.exception.object.domain.GameRoundHandler;
import com.kospot.exception.payload.code.ErrorStatus;
import com.kospot.presentation.multiGame.submission.dto.request.SubmissionRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Repl                                                                                                                                                                                                                                                                                                                        ace.NONE)
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class SubmitRoadViewPlayerAnswerUseCaseTest {

    @Mock
    private RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;

    @Mock
    private GamePlayerAdaptor gamePlayerAdaptor;

    @Mock
    private RoadViewPlayerSubmissionService roadViewPlayerSubmissionService;

    @InjectMocks
    private SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;

    // 테스트 데이터
    private Long roundId;
    private Long playerId;
    private SubmissionRequest.RoadViewPlayer request;
    private RoadViewGameRound round;
    private GamePlayer gamePlayer;
    private RoadViewPlayerSubmission submission;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        roundId = 1L;
        playerId = 2L;
        
        request = mock(SubmissionRequest.RoadViewPlayer.class);
        round = mock(RoadViewGameRound.class);
        gamePlayer = mock(GamePlayer.class);
        submission = mock(RoadViewPlayerSubmission.class);
        
        // 기본 스텁 설정
        given(request.getPlayerId()).willReturn(playerId);
        given(request.toEntity()).willReturn(submission);
        given(roadViewGameRoundAdaptor.queryById(roundId)).willReturn(round);
        given(gamePlayerAdaptor.queryById(playerId)).willReturn(gamePlayer);
        given(round.getId()).willReturn(roundId);
        given(gamePlayer.getId()).willReturn(playerId);
    }

    @Nested
    @DisplayName("정상 케이스 테스트")
    class HappyPathTests {

        @Test
        @DisplayName("유효한 요청으로 답변 제출 성공")
        void shouldSubmitAnswerSuccessfully() {
            // given
            doNothing().when(roadViewPlayerSubmissionService).createSubmission(round, gamePlayer, submission);

            // when
            assertDoesNotThrow(() -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));

            // then
            verify(roadViewGameRoundAdaptor).queryById(roundId);
            verify(gamePlayerAdaptor).queryById(playerId);
            verify(roadViewPlayerSubmissionService).createSubmission(round, gamePlayer, submission);
        }
    }

    @Nested
    @DisplayName("라운드 관련 예외 테스트")
    class RoundExceptionTests {

        @Test
        @DisplayName("존재하지 않는 라운드 ID로 요청 시 예외 발생")
        void shouldThrowExceptionWhenRoundNotFound() {
            // given
            given(roadViewGameRoundAdaptor.queryById(roundId)).willThrow(new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND));

            // when & then
            GameRoundHandler exception = assertThrows(GameRoundHandler.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
            assertEquals(ErrorStatus.GAME_ROUND_NOT_FOUND.getCode(), exception.getCode());
            verify(roadViewGameRoundAdaptor).queryById(roundId);
            verify(gamePlayerAdaptor, never()).queryById(any());
            verify(roadViewPlayerSubmissionService, never()).createSubmission(any(), any(), any());
        }

        @Test
        @DisplayName("이미 종료된 라운드에 제출 시 예외 발생")
        void shouldThrowExceptionWhenRoundAlreadyFinished() {
            // given
            doThrow(new GameRoundHandler(ErrorStatus.ROUND_ALREADY_FINISHED))
                    .when(roadViewPlayerSubmissionService).createSubmission(eq(round), eq(gamePlayer), any());

            // when & then
            GameRoundHandler exception = assertThrows(GameRoundHandler.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
            assertEquals(ErrorStatus.ROUND_ALREADY_FINISHED.getCode(), exception.getCode());
            verify(roadViewGameRoundAdaptor).queryById(roundId);
            verify(gamePlayerAdaptor).queryById(playerId);
            verify(roadViewPlayerSubmissionService).createSubmission(round, gamePlayer, submission);
        }
    }

    @Nested
    @DisplayName("플레이어 관련 예외 테스트")
    class PlayerExceptionTests {

        @Test
        @DisplayName("존재하지 않는 플레이어 ID로 요청 시 예외 발생")
        void shouldThrowExceptionWhenPlayerNotFound() {
            // given
            given(gamePlayerAdaptor.queryById(playerId)).willThrow(new GameRoundHandler(ErrorStatus.GAME_PLAYER_NOT_FOUND));

            // when & then
            GameRoundHandler exception = assertThrows(GameRoundHandler.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
            assertEquals(ErrorStatus.GAME_PLAYER_NOT_FOUND.getHttpStatus(),exception.getErrorReasonHttpStatus());
            verify(roadViewGameRoundAdaptor).queryById(roundId);
            verify(gamePlayerAdaptor).queryById(playerId);
            verify(roadViewPlayerSubmissionService, never()).createSubmission(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("제출 관련 예외 테스트")
    class SubmissionExceptionTests {

        @Test
        @DisplayName("이미 제출한 경우 예외 발생")
        void shouldThrowExceptionWhenAlreadySubmitted() {
            // given
            doThrow(new GameRoundHandler(ErrorStatus.ROUND_ALREADY_SUBMITTED))
                    .when(roadViewPlayerSubmissionService).createSubmission(eq(round), eq(gamePlayer), any());

            // when & then
            GameRoundHandler exception = assertThrows(GameRoundHandler.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
            assertEquals(ErrorStatus.ROUND_ALREADY_SUBMITTED.getCode(), exception.getCode());
            verify(roadViewGameRoundAdaptor).queryById(roundId);
            verify(gamePlayerAdaptor).queryById(playerId);
            verify(roadViewPlayerSubmissionService).createSubmission(round, gamePlayer, submission);
        }

        @Test
        @DisplayName("데이터베이스 유니크 제약 조건 위반 시 예외 발생")
        void shouldHandleUniqueConstraintViolation() {
            // given
            doThrow(new DataIntegrityViolationException("Unique constraint violation"))
                    .when(roadViewPlayerSubmissionService).createSubmission(eq(round), eq(gamePlayer), any());

            // when & then
            assertThrows(DataIntegrityViolationException.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
            verify(roadViewGameRoundAdaptor).queryById(roundId);
            verify(gamePlayerAdaptor).queryById(playerId);
            verify(roadViewPlayerSubmissionService).createSubmission(round, gamePlayer, submission);
        }
        
        @Test
        @DisplayName("요청 데이터 변환 실패 시 예외 발생")
        void shouldThrowExceptionWhenRequestConversionFails() {
            // given
            given(request.toEntity()).willThrow(new IllegalArgumentException("Invalid submission data"));

            // when & then
            assertThrows(IllegalArgumentException.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
            verify(roadViewGameRoundAdaptor).queryById(roundId);
            verify(gamePlayerAdaptor).queryById(playerId);
            verify(roadViewPlayerSubmissionService, never()).createSubmission(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("트랜잭션 관련 테스트")
    class TransactionTests {

        @Test
        @DisplayName("제출 중 예외 발생 시 트랜잭션 롤백 확인")
        void shouldRollbackTransactionOnException() {
            // given
            RuntimeException expectedException = new RuntimeException("Database connection error");
            doThrow(expectedException)
                    .when(roadViewPlayerSubmissionService).createSubmission(any(), any(), any());

            // when & then
            Exception exception = assertThrows(RuntimeException.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
            assertEquals(expectedException, exception);
            verify(roadViewGameRoundAdaptor).queryById(roundId);
            verify(gamePlayerAdaptor).queryById(playerId);
            verify(roadViewPlayerSubmissionService).createSubmission(round, gamePlayer, submission);
        }
    }

    @Nested
    @DisplayName("입력 값 검증 테스트")
    class ValidationTests {

        @Test
        @DisplayName("null 라운드 ID로 요청 시 예외 발생")
        void shouldThrowExceptionWhenRoundIdIsNull() {
            // when & then
            assertThrows(NullPointerException.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(null, request));
            verify(roadViewGameRoundAdaptor, never()).queryById(any());
            verify(gamePlayerAdaptor, never()).queryById(any());
            verify(roadViewPlayerSubmissionService, never()).createSubmission(any(), any(), any());
        }

        @Test
        @DisplayName("null 요청으로 호출 시 예외 발생")
        void shouldThrowExceptionWhenRequestIsNull() {
            // when & then
            assertThrows(NullPointerException.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, null));
            verify(roadViewGameRoundAdaptor, never()).queryById(any());
            verify(gamePlayerAdaptor, never()).queryById(any());
            verify(roadViewPlayerSubmissionService, never()).createSubmission(any(), any(), any());
        }
    }
}
