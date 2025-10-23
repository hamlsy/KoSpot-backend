package com.kospot.multi.round;

import com.kospot.application.coordinate.ImportCoordinateUseCase;
import com.kospot.application.multi.round.roadview.solo.StartRoadViewSoloRoundUseCase;
import com.kospot.application.multi.submission.http.usecase.SubmitRoadViewPlayerAnswerUseCase;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.repository.MultiRoadViewGameRepository;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.repository.GamePlayerRepository;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.repository.GameRoomRepository;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.repository.RoadViewGameRoundRepository;
import com.kospot.infrastructure.redis.domain.multi.room.adaptor.GameRoomRedisAdaptor;
import com.kospot.infrastructure.redis.domain.multi.submission.service.SubmissionRedisService;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import com.kospot.presentation.multi.submission.dto.request.SubmitRoadViewRequest;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 라운드 전환 및 다음 라운드 자동 시작 통합 테스트
 * 
 * RoundCompletionEventListener의 다음 기능들을 테스트:
 * 1. 라운드 종료 후 전환 타이머 시작 (startTransitionTimer)
 * 2. 전환 타이머 완료 후 다음 라운드 자동 시작 (handleNextRound)
 * 3. NextRoadViewRoundUseCase 호출하여 새 라운드 생성
 * 4. 마지막 라운드인 경우 게임 종료 (handleLastRound)
 * 
 * 검증 항목:
 * - 라운드 완료 → 전환 타이머 대기 → 다음 라운드 자동 시작
 * - 다음 라운드의 roundNumber가 증가했는지
 * - 다음 라운드의 타이머가 새로 시작되었는지
 * - 마지막 라운드 완료 시 게임이 종료되는지
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class RoundTransitionAndNextRoundTest {

    @Autowired
    private StartRoadViewSoloRoundUseCase startRoadViewSoloRoundUseCase;

    @Autowired
    private SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private MultiRoadViewGameRepository multiRoadViewGameRepository;

    @Autowired
    private RoadViewGameRoundRepository roadViewGameRoundRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImportCoordinateUseCase importCoordinateUseCase;

    @Autowired
    private SubmissionRedisService submissionRedisService;

    @Autowired
    private GameTimerService gameTimerService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @MockitoBean
    private GameRoomRedisAdaptor gameRoomRedisAdaptor;

    @Autowired
    private EntityManager entityManager;

    private Member hostMember;
    private List<Member> players;
    private GameRoom gameRoom;
    private Image markerImage;

    @BeforeEach
    void setUp() {
        // 마커 이미지 생성
        markerImage = Image.builder()
                .imageUrl("http://example.com/marker.png")
                .build();
        imageRepository.save(markerImage);

        // 멤버 생성 (4명)
        hostMember = createMember("host", "호스트");
        players = new ArrayList<>();
        players.add(hostMember);

        for (int i = 1; i <= 3; i++) {
            Member player = createMember("player" + i, "플레이어" + i);
            players.add(player);
        }

        // 게임방 생성
        gameRoom = createGameRoom(hostMember, players);

        // 좌표 데이터 import
        importCoordinateUseCase.execute("test_coordinates_excel.xlsx");

        // Redis Mock 설정
        when(gameRoomRedisAdaptor.getCurrentPlayers(anyString())).thenReturn(4L);

        log.info("✅ 테스트 환경 설정 완료 - 플레이어 수: {}, 방 ID: {}", 
                players.size(), gameRoom.getId());
    }

    @Test
    @DisplayName("[통합] 라운드 1 완료 후 자동으로 라운드 2로 전환된다")
    void whenRound1Completes_thenAutomaticallyTransitionsToRound2() throws InterruptedException {
        // Given: 게임 시작 (총 5라운드)
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), 60, 5);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long gameId = startResponse.getGameId();
        Long round1Id = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        log.info("🎮 게임 시작 - GameId: {}, Round1 ID: {}", gameId, round1Id);

        // Redis 초기화
        submissionRedisService.initializeRound(GameMode.ROADVIEW, round1Id);

        // 라운드 1 확인
        RoadViewGameRound round1 = roadViewGameRoundRepository.findById(round1Id).orElseThrow();
        assertThat(round1.getRoundNumber()).isEqualTo(1);
        assertThat(round1.getIsFinished()).isFalse();

        // When: 모든 플레이어가 제출하여 라운드 1 완료
        List<GamePlayer> gamePlayers = gamePlayerRepository.findAllByMultiRoadViewGameId(gameId);
        submitAllPlayers(gamePlayers, roomId, gameId, round1Id);

        log.info("✅ 모든 플레이어 제출 완료 - 라운드 1 종료 대기 중...");

        // 라운드 1 종료 확인 (조기 종료 이벤트 처리 대기)
        Thread.sleep(1000);
        entityManager.clear();

        round1 = roadViewGameRoundRepository.findById(round1Id).orElseThrow();
        assertThat(round1.getIsFinished()).isTrue();
        log.info("✅ 라운드 1 종료 확인");

        // Then: 전환 타이머 대기 후 라운드 2 자동 생성 확인
        // (전환 타이머는 GameTimerService의 ROUND_TRANSITION_DELAY 설정에 따라 3초)
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    entityManager.clear();
                    List<RoadViewGameRound> allRounds = roadViewGameRoundRepository
                            .findAllByMultiRoadViewGameId(gameId);
                    
                    log.info("🔍 현재 생성된 라운드 수: {}", allRounds.size());
                    
                    // 라운드 2가 생성되었는지 확인
                    assertThat(allRounds).hasSizeGreaterThanOrEqualTo(2);
                    
                    // 라운드 2 확인
                    RoadViewGameRound round2 = allRounds.stream()
                            .filter(r -> r.getRoundNumber() == 2)
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("라운드 2가 생성되지 않았습니다."));
                    
                    assertThat(round2.getRoundNumber()).isEqualTo(2);
                    assertThat(round2.getIsFinished()).isFalse();
                    assertThat(round2.getServerStartTime()).isNotNull();
                    
                    log.info("🎉 라운드 2 자동 생성 확인 - RoundId: {}, RoundNumber: {}", 
                            round2.getId(), round2.getRoundNumber());
                });

        // 게임 상태 확인
        MultiRoadViewGame game = multiRoadViewGameRepository.findById(gameId).orElseThrow();
        assertThat(game.getCurrentRound()).isEqualTo(2);
        assertThat(game.getIsFinished()).isFalse();
        log.info("✅ 게임 상태 확인 - 현재 라운드: {}, 종료여부: {}", 
                game.getCurrentRound(), game.getIsFinished());

        // Clean up
        stopAllTimers(roomId, gameId);
    }

    @Test
    @DisplayName("[통합] 마지막 라운드 완료 후 게임이 자동으로 종료된다")
    void whenLastRoundCompletes_thenGameFinishesAutomatically() throws InterruptedException {
        // Given: 게임 시작 (총 2라운드로 설정하여 빠른 테스트)
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), 60, 2);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long gameId = startResponse.getGameId();
        Long round1Id = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        log.info("🎮 게임 시작 - GameId: {}, 총 라운드: 2", gameId);

        // 라운드 1 완료
        submissionRedisService.initializeRound(GameMode.ROADVIEW, round1Id);
        List<GamePlayer> gamePlayers = gamePlayerRepository.findAllByMultiRoadViewGameId(gameId);
        submitAllPlayers(gamePlayers, roomId, gameId, round1Id);
        Thread.sleep(1000);

        log.info("✅ 라운드 1 완료");

        // 라운드 2 시작 대기
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    entityManager.clear();
                    List<RoadViewGameRound> allRounds = roadViewGameRoundRepository
                            .findAllByMultiRoadViewGameId(gameId);
                    assertThat(allRounds).hasSizeGreaterThanOrEqualTo(2);
                });

        entityManager.clear();
        List<RoadViewGameRound> allRounds = roadViewGameRoundRepository
                .findAllByMultiRoadViewGameId(gameId);
        RoadViewGameRound round2 = allRounds.stream()
                .filter(r -> r.getRoundNumber() == 2)
                .findFirst()
                .orElseThrow();

        log.info("🎮 라운드 2 시작 - RoundId: {}", round2.getId());

        // When: 라운드 2 (마지막 라운드) 완료
        submissionRedisService.initializeRound(GameMode.ROADVIEW, round2.getId());
        submitAllPlayers(gamePlayers, roomId, gameId, round2.getId());
        Thread.sleep(1000);

        log.info("✅ 라운드 2 (마지막) 완료");

        // Then: 전환 타이머 대기 후 게임 종료 확인
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    entityManager.clear();
                    MultiRoadViewGame game = multiRoadViewGameRepository.findById(gameId).orElseThrow();
                    
                    log.info("🔍 게임 상태 확인 - 종료여부: {}, 현재 라운드: {}/{}", 
                            game.getIsFinished(), game.getCurrentRound(), game.getTotalRounds());
                    
                    // 게임이 종료되었는지 확인
                    assertThat(game.getIsFinished()).isTrue();
                    assertThat(game.getCurrentRound()).isEqualTo(2);
                    
                    log.info("🏁 게임 종료 확인!");
                });

        // 마지막 라운드도 종료되었는지 확인
        entityManager.clear();
        round2 = roadViewGameRoundRepository.findById(round2.getId()).orElseThrow();
        assertThat(round2.getIsFinished()).isTrue();

        log.info("✅ 게임 종료 테스트 완료");

        // Clean up
        stopAllTimers(roomId, gameId);
    }

    @Test
    @DisplayName("[통합] 각 라운드마다 독립적인 타이머가 시작된다")
    void eachRound_HasIndependentTimer() throws InterruptedException {
        // Given: 게임 시작 (타이머 30초)
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), 30, 3);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long gameId = startResponse.getGameId();
        Long round1Id = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        log.info("🎮 게임 시작 - 타이머: 30초");

        // 라운드 1 타이머 확인
        submissionRedisService.initializeRound(GameMode.ROADVIEW, round1Id);
        RoadViewGameRound round1 = roadViewGameRoundRepository.findById(round1Id).orElseThrow();
        assertThat(round1.getServerStartTime()).isNotNull();
        assertThat(round1.getRemainingTimeMs()).isLessThanOrEqualTo(30000);
        log.info("⏱️  라운드 1 타이머 시작 - 남은시간: {}ms", round1.getRemainingTimeMs());

        // When: 라운드 1 완료
        List<GamePlayer> gamePlayers = gamePlayerRepository.findAllByMultiRoadViewGameId(gameId);
        submitAllPlayers(gamePlayers, roomId, gameId, round1Id);
        Thread.sleep(1000);

        // 라운드 2 생성 대기
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    entityManager.clear();
                    List<RoadViewGameRound> allRounds = roadViewGameRoundRepository
                            .findAllByMultiRoadViewGameId(gameId);
                    assertThat(allRounds).hasSizeGreaterThanOrEqualTo(2);
                });

        // Then: 라운드 2 타이머가 새로 시작되었는지 확인
        entityManager.clear();
        List<RoadViewGameRound> allRounds = roadViewGameRoundRepository
                .findAllByMultiRoadViewGameId(gameId);
        RoadViewGameRound round2 = allRounds.stream()
                .filter(r -> r.getRoundNumber() == 2)
                .findFirst()
                .orElseThrow();

        assertThat(round2.getServerStartTime()).isNotNull();
        assertThat(round2.getRemainingTimeMs()).isLessThanOrEqualTo(30000);
        assertThat(round2.getServerStartTime()).isAfter(round1.getServerStartTime());
        
        log.info("⏱️  라운드 2 타이머 시작 - 남은시간: {}ms", round2.getRemainingTimeMs());
        log.info("✅ 각 라운드 독립적 타이머 확인 완료");

        // Clean up
        stopAllTimers(roomId, gameId);
    }

    @Test
    @DisplayName("[통합] 타이머 만료로 라운드가 종료되어도 다음 라운드로 전환된다")
    void whenRoundEndsWithTimerExpiration_thenStillTransitionsToNextRound() throws InterruptedException {
        // Given: 짧은 타이머(5초)로 게임 시작
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), 5, 3);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long gameId = startResponse.getGameId();
        Long round1Id = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        log.info("🎮 게임 시작 - 타이머: 5초 (타이머 만료 대기)");

        submissionRedisService.initializeRound(GameMode.ROADVIEW, round1Id);

        // When: 타이머 만료 대기 (아무도 제출하지 않음)
        await()
                .atMost(8, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    entityManager.clear();
                    RoadViewGameRound round1 = roadViewGameRoundRepository.findById(round1Id).orElseThrow();
                    log.info("🔍 라운드 1 상태 - 남은시간: {}ms, 종료: {}", 
                            round1.getRemainingTimeMs(), round1.getIsFinished());
                    assertThat(round1.isTimeExpired()).isTrue();
                });

        log.info("⏰ 라운드 1 타이머 만료 확인");

        // Then: 전환 타이머 후 라운드 2 생성 확인
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    entityManager.clear();
                    List<RoadViewGameRound> allRounds = roadViewGameRoundRepository
                            .findAllByMultiRoadViewGameId(gameId);
                    
                    log.info("🔍 타이머 만료 후 라운드 수: {}", allRounds.size());
                    
                    assertThat(allRounds).hasSizeGreaterThanOrEqualTo(2);
                    
                    RoadViewGameRound round2 = allRounds.stream()
                            .filter(r -> r.getRoundNumber() == 2)
                            .findFirst()
                            .orElseThrow();
                    
                    assertThat(round2.getRoundNumber()).isEqualTo(2);
                    assertThat(round2.getIsFinished()).isFalse();
                    
                    log.info("🎉 타이머 만료 후에도 라운드 2로 전환 확인!");
                });

        // Clean up
        stopAllTimers(roomId, gameId);
    }

    @Test
    @DisplayName("[통합] 연속으로 여러 라운드가 자동 전환된다")
    void multipleRounds_TransitionAutomatically() throws InterruptedException {
        // Given: 게임 시작 (총 4라운드)
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), 60, 4);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long gameId = startResponse.getGameId();
        String roomId = gameRoom.getId().toString();

        log.info("🎮 게임 시작 - 총 4라운드 연속 테스트");

        List<GamePlayer> gamePlayers = gamePlayerRepository.findAllByMultiRoadViewGameId(gameId);

        // When: 라운드 1, 2, 3을 순차적으로 완료
        for (int roundNum = 1; roundNum <= 3; roundNum++) {
            final int currentRound = roundNum;
            
            // 현재 라운드 찾기
            await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        entityManager.clear();
                        List<RoadViewGameRound> allRounds = roadViewGameRoundRepository
                                .findAllByMultiRoadViewGameId(gameId);
                        assertThat(allRounds).hasSizeGreaterThanOrEqualTo(currentRound);
                    });

            entityManager.clear();
            List<RoadViewGameRound> allRounds = roadViewGameRoundRepository
                    .findAllByMultiRoadViewGameId(gameId);
            RoadViewGameRound currentRoundEntity = allRounds.stream()
                    .filter(r -> r.getRoundNumber() == currentRound)
                    .findFirst()
                    .orElseThrow();

            log.info("📍 라운드 {} 진행 중 - RoundId: {}", currentRound, currentRoundEntity.getId());

            // 라운드 완료
            submissionRedisService.initializeRound(GameMode.ROADVIEW, currentRoundEntity.getId());
            submitAllPlayers(gamePlayers, roomId, gameId, currentRoundEntity.getId());
            Thread.sleep(1000);

            log.info("✅ 라운드 {} 완료", currentRound);
        }

        // Then: 라운드 4가 자동 생성되었는지 확인
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    entityManager.clear();
                    List<RoadViewGameRound> allRounds = roadViewGameRoundRepository
                            .findAllByMultiRoadViewGameId(gameId);
                    
                    log.info("🔍 최종 라운드 수: {}", allRounds.size());
                    assertThat(allRounds).hasSize(4);
                    
                    // 각 라운드 번호 확인
                    for (int i = 1; i <= 4; i++) {
                        final int roundNum = i;
                        boolean exists = allRounds.stream()
                                .anyMatch(r -> r.getRoundNumber() == roundNum);
                        assertThat(exists).as("라운드 " + roundNum + " 존재").isTrue();
                    }
                    
                    log.info("🎉 4개 라운드 모두 생성 확인!");
                });

        // 게임 상태 확인
        MultiRoadViewGame game = multiRoadViewGameRepository.findById(gameId).orElseThrow();
        assertThat(game.getCurrentRound()).isEqualTo(4);
        assertThat(game.getIsFinished()).isFalse();

        log.info("✅ 연속 라운드 전환 테스트 완료");

        // Clean up
        stopAllTimers(roomId, gameId);
    }

    // === Helper Methods ===

    private Member createMember(String username, String nickname) {
        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .equippedMarkerImage(markerImage)
                .role(Role.USER)
                .build();
        return memberRepository.save(member);
    }

    private GameRoom createGameRoom(Member host, List<Member> members) {
        GameRoom room = GameRoom.builder()
                .host(host)
                .gameMode(GameMode.ROADVIEW)
                .playerMatchType(PlayerMatchType.SOLO)
                .maxPlayers(6)
                .status(GameRoomStatus.WAITING)
                .title("라운드 전환 테스트 방")
                .build();

        GameRoom savedRoom = gameRoomRepository.save(room);

        for (Member member : members) {
            savedRoom.join(member, null);
        }
        memberRepository.saveAll(members);
        return gameRoomRepository.save(savedRoom);
    }

    private MultiGameRequest.Start createStartRequest(Long gameRoomId, Integer timeLimit, Integer totalRounds) {
        MultiGameRequest.Start request = new MultiGameRequest.Start();
        request.setGameRoomId(gameRoomId);
        request.setTotalRounds(totalRounds);
        request.setTimeLimit(timeLimit);
        request.setPlayerMatchTypeKey("SOLO");
        return request;
    }

    /**
     * 모든 플레이어가 제출하도록 헬퍼 메서드
     */
    private void submitAllPlayers(List<GamePlayer> gamePlayers, String roomId, 
                                  Long gameId, Long roundId) {
        for (int i = 0; i < gamePlayers.size(); i++) {
            GamePlayer gamePlayer = gamePlayers.get(i);
            Member member = memberRepository.findById(gamePlayer.getMemberId()).orElseThrow();

            SubmitRoadViewRequest.Player submitRequest = SubmitRoadViewRequest.Player.builder()
                    .lat(37.5665 + (i * 0.01))
                    .lng(126.9780 + (i * 0.01))
                    .timeToAnswer(5000.0 + (i * 1000.0))
                    .build();

            submitRoadViewPlayerAnswerUseCase.execute(member, roomId, gameId, roundId, submitRequest);
        }
    }

    /**
     * 모든 타이머 중지 (테스트 정리용)
     */
    private void stopAllTimers(String roomId, Long gameId) {
        try {
            List<RoadViewGameRound> allRounds = roadViewGameRoundRepository
                    .findAllByMultiRoadViewGameId(gameId);
            for (RoadViewGameRound round : allRounds) {
                gameTimerService.stopRoundTimer(roomId, round);
            }
            log.info("🧹 모든 타이머 정리 완료");
        } catch (Exception e) {
            log.warn("⚠️ 타이머 정리 중 예외: {}", e.getMessage());
        }
    }
}

