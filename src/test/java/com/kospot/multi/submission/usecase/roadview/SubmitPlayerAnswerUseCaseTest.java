package com.kospot.multi.submission.usecase.roadview;

import com.kospot.application.multi.submission.http.usecase.SubmitRoadViewPlayerAnswerUseCase;
import com.kospot.domain.coordinate.entity.Address;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;

import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinate.repository.nationwide.CoordinateNationwideRepository;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.repository.MultiRoadViewGameRepository;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.vo.GamePlayerStatus;
import com.kospot.domain.multi.gamePlayer.repository.GamePlayerRepository;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.repository.RoadViewGameRoundRepository;
import com.kospot.domain.multi.submission.entity.roadview.RoadViewPlayerSubmission;
import com.kospot.domain.multi.submission.repository.RoadViewPlayerSubmissionRepository;
import com.kospot.infrastructure.exception.object.domain.GameRoundHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.presentation.multi.submission.dto.request.SubmitRoadViewRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubmitPlayerAnswerUseCaseTest {

    @Autowired
    private SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;

    @Autowired
    private RoadViewGameRoundRepository roadViewGameRoundRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private RoadViewPlayerSubmissionRepository roadViewPlayerSubmissionRepository;

    @Autowired
    private MultiRoadViewGameRepository multiRoadViewGameRepository;

    @Autowired
    private CoordinateNationwideRepository coordinateNationwideRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // 테스트 데이터
    private Long roundId;
    private Long playerId;
    private SubmitRoadViewRequest.Player request;
    private RoadViewGameRound round;
    private GamePlayer gamePlayer;
    private MultiRoadViewGame game;
    private CoordinateNationwide coordinate;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화 및 저장
        createTestData();
        
        // 요청 객체 생성
        request = createSubmissionRequest();
    }

    private void createTestData() {
        // 좌표 생성 및 저장
        coordinate = CoordinateNationwide.builder()
                .lat(37.5665)
                .lng(126.9780)
                .address(new Address(Sido.SEOUL, "중구", "세종대로 110"))
                .build();
        coordinateNationwideRepository.save(coordinate);

        // 게임 생성 및 저장
        game = MultiRoadViewGame.builder()
                .title("테스트 게임")
                .totalRounds(5)
                .currentRound(1)
                .isFinished(false)
                .build();

        multiRoadViewGameRepository.save(game);

        // 플레이어 생성 및 저장
        gamePlayer = GamePlayer.builder()
                .nickname("테스트 플레이어")
                .status(GamePlayerStatus.PLAYING)
                .totalScore(0)
                .build();
        gamePlayerRepository.save(gamePlayer);

        // ID 저장
        roundId = round.getId();
        playerId = gamePlayer.getId();

        // 라운드 생성 및 저장
        round = RoadViewGameRound.createRound(1, coordinate, 120, List.of(playerId));
        round.setMultiRoadViewGame(game);

        roadViewGameRoundRepository.save(round);


        // 영속성 컨텍스트 초기화
        flushAndClear();
    }

    private SubmitRoadViewRequest.Player createSubmissionRequest() {
        SubmitRoadViewRequest.Player request = SubmitRoadViewRequest.Player.builder()
                .distance(100.5)
                .lng(126.9780)
                .lat(37.5665)
                .build();
        return request;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("정상 케이스 테스트")
    class HappyPathTests {

        @Test
        @DisplayName("유효한 요청으로 답변 제출 성공")
        void shouldSubmitAnswerSuccessfully() {
            // when
            assertDoesNotThrow(() -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));

            // then
            flushAndClear();
            
            // 제출이 저장되었는지 확인
            assertTrue(roadViewPlayerSubmissionRepository.existsByRoundIdAndGamePlayerId(roundId, playerId));
            
            // 저장된 제출 데이터 검증
            List<RoadViewPlayerSubmission> submissions = roadViewPlayerSubmissionRepository.findAll();
            assertFalse(submissions.isEmpty());
            
            RoadViewPlayerSubmission submission = submissions.stream()
                .filter(s -> s.getGamePlayer().getId().equals(playerId) && s.getRoadViewGameRound().getId().equals(roundId))
                .findFirst()
                .orElseThrow();
            
            assertEquals(request.getLat(), submission.getLat());
            assertEquals(request.getLng(), submission.getLng());
            assertEquals(request.getDistance(), submission.getDistance());

        }

        @Test
        @DisplayName("여러 플레이어가 동일 라운드에 제출 성공")
        void shouldAllowMultiplePlayersToSubmit() {
            // given
            // 첫 번째 플레이어 제출
            submitRoadViewPlayerAnswerUseCase.execute(roundId, request);
            flushAndClear();
            
            // 두 번째 플레이어 생성 및 제출
            GamePlayer secondPlayer = GamePlayer.builder()
                .nickname("두 번째 플레이어")
                .status(GamePlayerStatus.PLAYING)
                .totalScore(0)
                .build();
            gamePlayerRepository.save(secondPlayer);
            
            SubmitRoadViewRequest.Player secondRequest = new SubmitRoadViewRequest.Player();
            secondRequest.setPlayerId(secondPlayer.getId());
            secondRequest.setLat(37.5660);
            secondRequest.setLng(126.9775);
            secondRequest.setDistance(120.5);
            secondRequest.setTimeToAnswer(10.3);
            
            // when
            assertDoesNotThrow(() -> submitRoadViewPlayerAnswerUseCase.execute(roundId, secondRequest));
            
            // then
            flushAndClear();
            
            // 두 개의 제출이 저장되었는지 확인
            List<RoadViewPlayerSubmission> submissions = roadViewPlayerSubmissionRepository.findAll();
            assertEquals(2, submissions.size());
        }
    }

    @Nested
    @DisplayName("라운드 관련 예외 테스트")
    class RoundExceptionTests {

        @Test
        @DisplayName("존재하지 않는 라운드 ID로 요청 시 예외 발생")
        void shouldThrowExceptionWhenRoundNotFound() {
            // when & then
            Exception exception = assertThrows(Exception.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(999L, request));
            
            // 실제 예외 타입과 메시지는 구현에 따라 다를 수 있음
            assertNotNull(exception);
        }

        @Test
        @DisplayName("이미 종료된 라운드에 제출 시 예외 발생")
        void shouldThrowExceptionWhenRoundAlreadyFinished() {
            // given
            round = roadViewGameRoundRepository.findById(roundId).orElseThrow();
            round.finishRound(); // 라운드 종료 처리
            roadViewGameRoundRepository.save(round);
            flushAndClear();

            // when & then
            GameRoundHandler exception = assertThrows(GameRoundHandler.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
            
            assertEquals(ErrorStatus.ROUND_ALREADY_FINISHED.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("플레이어 관련 예외 테스트")
    class PlayerExceptionTests {

        @Test
        @DisplayName("존재하지 않는 플레이어 ID로 요청 시 예외 발생")
        void shouldThrowExceptionWhenPlayerNotFound() {
            // given
            request.setPlayerId(999L);

            // when & then
            Exception exception = assertThrows(Exception.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
            
            // 실제 예외 타입과 메시지는 구현에 따라 다를 수 있음
            assertNotNull(exception);
        }
    }

    @Nested
    @DisplayName("제출 관련 예외 테스트")
    class SubmissionExceptionTests {

        @Test
        @DisplayName("이미 제출한 플레이어가 다시 제출 시 예외 발생")
        void shouldThrowExceptionWhenAlreadySubmitted() {
            // given - 먼저 한 번 제출
            submitRoadViewPlayerAnswerUseCase.execute(roundId, request);
            flushAndClear();

            // when & then - 같은 플레이어가 같은 라운드에 다시 제출
            GameRoundHandler exception = assertThrows(GameRoundHandler.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
            
            assertEquals(ErrorStatus.ROUND_ALREADY_SUBMITTED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("유효하지 않은 요청 데이터로 제출 시 예외 발생")
        void shouldThrowExceptionWhenRequestDataInvalid() {
            // given
            request.setLat(null); // 필수 데이터를 null로 설정
            
            // when & then
            assertThrows(Exception.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
        }

        @Test
        @DisplayName("거리 값이 음수인 경우 예외 발생")
        void shouldThrowExceptionWhenDistanceIsNegative() {
            // given
            request.setDistance(-10.0); // 음수 거리 설정
            
            // when & then
            assertThrows(Exception.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
        }
    }

    @Nested
    @DisplayName("트랜잭션 관련 테스트")
    class TransactionTests {

        @Test
        @DisplayName("제출 중 예외 발생 시 트랜잭션 롤백 확인")
        void shouldRollbackTransactionOnException() {
            // given
            // 제출 전 데이터베이스 상태 확인
            int submissionCountBefore = roadViewPlayerSubmissionRepository.findAll().size();
            
            // 예외를 발생시키기 위해 라운드를 종료 상태로 변경
            round = roadViewGameRoundRepository.findById(roundId).orElseThrow();
            round.finishRound();
            roadViewGameRoundRepository.save(round);
            flushAndClear();
            
            // when
            assertThrows(GameRoundHandler.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
            
            // then
            flushAndClear();
            
            // 롤백 확인 - 제출 데이터가 추가되지 않아야 함
            int submissionCountAfter = roadViewPlayerSubmissionRepository.findAll().size();
            assertEquals(submissionCountBefore, submissionCountAfter);
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
        }

        @Test
        @DisplayName("null 요청으로 호출 시 예외 발생")
        void shouldThrowExceptionWhenRequestIsNull() {
            // when & then
            assertThrows(NullPointerException.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, null));
        }
        
        @Test
        @DisplayName("플레이어 ID가 null인 경우 예외 발생")
        void shouldThrowExceptionWhenPlayerIdIsNull() {
            // given
            request.setPlayerId(null);
            
            // when & then
            assertThrows(Exception.class, 
                    () -> submitRoadViewPlayerAnswerUseCase.execute(roundId, request));
        }
    }
    
    @Nested
    @DisplayName("성능 및 부하 테스트")
    class PerformanceTests {

        @Test
        @DisplayName("다수의 플레이어가 동시에 제출할 때 성능 확인")
        void shouldHandleMultipleSubmissionsEfficiently() {
            // given
            int playerCount = 5; // 테스트용 플레이어 수 (실제 환경에서는 더 많이 설정 가능)
            GamePlayer[] players = new GamePlayer[playerCount];
            
            // 여러 플레이어 생성
            for (int i = 0; i < playerCount; i++) {
                players[i] = GamePlayer.builder()
                    .nickname("플레이어 " + i)
                    .status(GamePlayerStatus.PLAYING)
                    .totalScore(0)
                    .build();
                gamePlayerRepository.save(players[i]);
            }
            flushAndClear();
            
            // when & then
            for (int i = 0; i < playerCount; i++) {
                final int index = i;
                SubmitRoadViewRequest.Player playerRequest = new SubmitRoadViewRequest.Player();
                playerRequest.setPlayerId(players[index].getId());
                playerRequest.setLat(37.5665 + (index * 0.001));
                playerRequest.setLng(126.9780 + (index * 0.001));
                playerRequest.setDistance(100.0 + index);
                playerRequest.setTimeToAnswer(10.0 + index);
                
                assertDoesNotThrow(() -> submitRoadViewPlayerAnswerUseCase.execute(roundId, playerRequest));
            }
            
            flushAndClear();
            
            // 모든 제출이 저장되었는지 확인
            List<RoadViewPlayerSubmission> submissions = roadViewPlayerSubmissionRepository.findAll();
            assertEquals(playerCount, submissions.size());
        }
    }
}
