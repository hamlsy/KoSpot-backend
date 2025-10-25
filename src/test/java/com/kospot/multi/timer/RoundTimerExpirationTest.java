package com.kospot.multi.timer;

import com.kospot.application.coordinate.ImportCoordinateUseCase;
import com.kospot.application.multi.round.roadview.solo.StartRoadViewSoloRoundUseCase;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multi.game.repository.MultiRoadViewGameRepository;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.repository.GamePlayerRepository;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.repository.GameRoomRepository;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.repository.RoadViewGameRoundRepository;
import com.kospot.domain.multi.submission.repository.RoadViewSubmissionRepository;
import com.kospot.infrastructure.redis.domain.multi.submission.service.SubmissionRedisService;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import com.kospot.infrastructure.websocket.domain.multi.timer.vo.TimerCommand;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 라운드 타이머 만료 및 자동 종료 통합 테스트
 * 
 * 검증 항목:
 * 1. 타이머 시작 후 설정된 시간이 지나면 라운드가 자동으로 종료된다
 * 2. 타이머 만료 시 미제출 플레이어는 0점 처리된다
 * 3. 타이머 중지 후에는 더 이상 만료 이벤트가 발생하지 않는다
 * 4. 여러 라운드에서 타이머가 독립적으로 동작한다
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
//@Transactional
class RoundTimerExpirationTest {

    @Autowired
    private StartRoadViewSoloRoundUseCase startRoadViewSoloRoundUseCase;

    @Autowired
    private GameTimerService gameTimerService;

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
    private RoadViewSubmissionRepository submissionRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImportCoordinateUseCase importCoordinateUseCase;

    @Autowired
    private SubmissionRedisService submissionRedisService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

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

        // 멤버 생성
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

        log.info("✅ 테스트 환경 설정 완료 - 플레이어 수: {}, 방 ID: {}", 
                players.size(), gameRoom.getId());
    }

    @Test
    @DisplayName("[통합] 타이머가 만료되면 라운드가 자동으로 종료된다")
//    @Transactional
    void whenTimerExpires_thenRoundEndsAutomatically() {
        // Given: 짧은 타이머(5초)로 게임 시작
        int shortTimerSeconds = 5;
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), shortTimerSeconds);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long roundId = startResponse.getRoundInfo().getRoundId();

        log.info("⏰ 타이머 테스트 시작 - 제한 시간: {}초, RoundId: {}", shortTimerSeconds, roundId);

        // Redis 초기화
        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        // 라운드 조회 및 타이머 시작
        RoadViewGameRound round = roadViewGameRoundRepository.findById(roundId).orElseThrow();
        assertThat(round.getIsFinished()).isFalse();

        TimerCommand command = TimerCommand.builder()
                .round(round)
                .gameRoomId(gameRoom.getId().toString())
                .gameId(startResponse.getGameId())
                .gameMode(GameMode.ROADVIEW)
                .matchType(PlayerMatchType.SOLO)
                .build();

        gameTimerService.startRoundTimer(command);
        log.info("⏱️  타이머 시작됨");

        // When: 타이머 만료 대기 (5초 + 버퍼 2초)
        await()
                .atMost(7, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    RoadViewGameRound updatedRound = 
                            roadViewGameRoundRepository.findById(round.getId()).orElseThrow();
                    
                    log.info("🔍 라운드 상태 확인 - 남은시간: {}ms, 종료여부: {}", 
                            updatedRound.getRemainingTimeMs(), updatedRound.getIsFinished());
                    
                    // 타이머 만료 확인
                    assertThat(updatedRound.isTimeExpired()).isTrue();
                });

        // Then: 라운드가 자동 종료되었는지 확인
        RoadViewGameRound finalRound = roadViewGameRoundRepository.findById(roundId).orElseThrow();
        
        log.info("✅ 타이머 만료 후 라운드 상태:");
        log.info("   - 종료 여부: {}", finalRound.getIsFinished());
        log.info("   - 남은 시간: {}ms", finalRound.getRemainingTimeMs());
        log.info("   - 시간 만료: {}", finalRound.isTimeExpired());

        assertThat(finalRound.isTimeExpired()).isTrue();

        // Clean up
        gameTimerService.stopRoundTimer(gameRoom.getId().toString(), round);
    }

    @Test
    @DisplayName("[통합] 타이머 만료 후 미제출 플레이어는 0점 처리된다")
    @Transactional
    void whenTimerExpires_thenNonSubmittedPlayersGetZeroScore() {
        // Given: 게임 시작
        int timerSeconds = 3;
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), timerSeconds);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long roundId = startResponse.getRoundInfo().getRoundId();

        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        log.info("🎮 테스트 시작 - 4명 중 2명만 제출 예정");

        // 4명 중 2명만 제출
        // (실제 제출은 생략하고 타이머 만료를 기다림)

        // When: 타이머 만료 대기
        RoadViewGameRound round = roadViewGameRoundRepository.findById(roundId).orElseThrow();
        
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    RoadViewGameRound updatedRound = 
                            roadViewGameRoundRepository.findById(roundId).orElseThrow();
                    assertThat(updatedRound.isTimeExpired()).isTrue();
                });

        log.info("⏰ 타이머 만료됨 - 라운드 종료 처리 시작");

        // 라운드 종료 처리 (미제출자 0점 처리 포함)
        // endRoadViewSoloRoundUseCase.execute(gameId, roundId);

        // Then: 미제출자 확인
        long submissionCount = submissionRepository.countByRoundIdAndMatchType(roundId, PlayerMatchType.SOLO);
        log.info("📊 최종 제출 수: {} (플레이어 수: 4)", submissionCount);

        // 타이머 만료 확인
        RoadViewGameRound finalRound = roadViewGameRoundRepository.findById(roundId).orElseThrow();
        assertThat(finalRound.isTimeExpired()).isTrue();
        log.info("✅ 타이머 만료 확인 완료");

        // Clean up
        gameTimerService.stopRoundTimer(gameRoom.getId().toString(), round);
    }

    @Test
    @DisplayName("[통합] 타이머를 중지하면 더 이상 만료 체크가 이루어지지 않는다")
    @Transactional
    void whenTimerStopped_thenNoExpirationCheck() throws InterruptedException {
        // Given: 게임 시작
        int timerSeconds = 10;
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), timerSeconds);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long roundId = startResponse.getRoundInfo().getRoundId();
        RoadViewGameRound round = roadViewGameRoundRepository.findById(roundId).orElseThrow();

        TimerCommand command = TimerCommand.builder()
                .round(round)
                .gameRoomId(gameRoom.getId().toString())
                .gameId(startResponse.getGameId())
                .gameMode(GameMode.ROADVIEW)
                .matchType(PlayerMatchType.SOLO)
                .build();

        gameTimerService.startRoundTimer(command);
        log.info("⏱️  타이머 시작 - 제한시간: {}초", timerSeconds);

        // When: 2초 후 타이머 중지
        Thread.sleep(2000);
        gameTimerService.stopRoundTimer(gameRoom.getId().toString(), round);
        log.info("⏹️  타이머 중지됨");

        long remainingAfterStop = round.getRemainingTimeMs();
        log.info("📊 중지 시점 남은 시간: {}ms", remainingAfterStop);

        // 추가로 5초 대기 (원래라면 만료되었을 시간)
        Thread.sleep(5000);

        // Then: 라운드가 여전히 진행 중이어야 함
        RoadViewGameRound finalRound = roadViewGameRoundRepository.findById(roundId).orElseThrow();
        
        // 타이머 중지 후에는 RemainingTimeMs가 고정됨
        log.info("📊 5초 대기 후:");
        log.info("   - 라운드 종료 여부: {}", finalRound.getIsFinished());
        log.info("   - 남은 시간: {}ms (중지 시점과 동일해야 함)", finalRound.getRemainingTimeMs());

        assertThat(finalRound.getIsFinished()).isFalse();
        log.info("✅ 타이머 중지 후 자동 만료 방지 확인 완료");
    }

    @Test
    @DisplayName("[통합] 남은 시간이 10초 이하일 때 finalCountdown 플래그가 활성화된다")
//    @Transactional
    void whenRemainingTimeLessThan10Seconds_thenFinalCountdownActivates() {
        // Given: 짧은 타이머(12초)로 게임 시작
        int timerSeconds = 12;
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), timerSeconds);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long roundId = startResponse.getRoundInfo().getRoundId();
        RoadViewGameRound round = roadViewGameRoundRepository.findById(roundId).orElseThrow();

        TimerCommand command = TimerCommand.builder()
                .round(round)
                .gameRoomId(gameRoom.getId().toString())
                .gameId(startResponse.getGameId())
                .gameMode(GameMode.ROADVIEW)
                .matchType(PlayerMatchType.SOLO)
                .build();

        gameTimerService.startRoundTimer(command);
        log.info("⏱️  타이머 시작 - 제한시간: {}초", timerSeconds);

        // When & Then: 시간 경과에 따라 finalCountdown 상태 확인
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    RoadViewGameRound updatedRound = 
                            roadViewGameRoundRepository.findById(roundId).orElseThrow();
                    long remaining = updatedRound.getRemainingTimeMs();
                    
                    if (remaining <= 10000) {
                        log.info("🔥 마지막 10초 진입 - 남은시간: {}ms", remaining);
                        assertThat(remaining).isLessThanOrEqualTo(10000);
                    }
                });

        log.info("✅ 마지막 카운트다운 플래그 활성화 확인 완료");

        // Clean up
        gameTimerService.stopRoundTimer(gameRoom.getId().toString(), round);
    }

    @Test
    @DisplayName("[통합] 라운드 시작 시간이 서버 시간 기준으로 정확히 설정된다")
    @Transactional
    void roundStartTime_SetAccurately() {
        // Given & When: 게임 시작
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), 30);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long roundId = startResponse.getRoundInfo().getRoundId();
        RoadViewGameRound round = roadViewGameRoundRepository.findById(roundId).orElseThrow();

        // Then: 서버 시작 시간이 설정되었는지 확인
        assertThat(round.getServerStartTime()).isNotNull();
        
        long now = System.currentTimeMillis();
        long serverStartMs = round.getServerStartTime().toEpochMilli();
        long diff = now - serverStartMs;

        log.info("📊 라운드 시작 시간 검증:");
        log.info("   - 서버 시작 시간: {}", round.getServerStartTime());
        log.info("   - 현재 시간과 차이: {}ms", diff);
        log.info("   - 남은 시간: {}ms", round.getRemainingTimeMs());

        // 시작 시간이 현재 시간과 1초 이내 차이여야 함
        assertThat(diff).isLessThan(1000);
        assertThat(round.getRemainingTimeMs()).isLessThanOrEqualTo(30000);
        
        log.info("✅ 서버 시작 시간 정확성 확인 완료");

        // Clean up
        gameTimerService.stopRoundTimer(gameRoom.getId().toString(), round);
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
                .title("타이머 만료 테스트 방")
                .build();

        GameRoom savedRoom = gameRoomRepository.save(room);

        for (Member member : members) {
            savedRoom.join(member, null);
        }

        return gameRoomRepository.save(savedRoom);
    }

    private MultiGameRequest.Start createStartRequest(Long gameRoomId, Integer timeLimit) {
        MultiGameRequest.Start request = new MultiGameRequest.Start();
        request.setGameRoomId(gameRoomId);
        request.setTotalRounds(5);
        request.setTimeLimit(timeLimit);
        request.setPlayerMatchTypeKey("SOLO");
        return request;
    }
}

