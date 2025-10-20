package com.kospot.multi.submission;

import com.kospot.application.coordinate.ImportCoordinateUseCase;
import com.kospot.application.multi.round.roadview.CheckAndCompleteRoundEarlyUseCase;
import com.kospot.application.multi.round.roadview.solo.EndRoadViewSoloRoundUseCase;
import com.kospot.application.multi.round.roadview.solo.StartRoadViewSoloRoundUseCase;
import com.kospot.application.multi.submission.http.usecase.SubmitRoadViewPlayerAnswerUseCase;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multi.game.repository.MultiRoadViewGameRepository;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.repository.GamePlayerRepository;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.repository.GameRoomRepository;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.repository.RoadViewGameRoundRepository;
import com.kospot.domain.multi.submission.entity.roadview.RoadViewSubmission;
import com.kospot.domain.multi.submission.repository.RoadViewSubmissionRepository;
import com.kospot.infrastructure.redis.domain.multi.room.adaptor.GameRoomRedisAdaptor;
import com.kospot.infrastructure.redis.domain.multi.submission.service.SubmissionRedisService;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import com.kospot.presentation.multi.round.dto.response.RoadViewRoundResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 로드뷰 제출 및 조기 종료 통합 테스트
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class RoadViewSubmissionEarlyCompletionTest {

    @Autowired
    private StartRoadViewSoloRoundUseCase startRoadViewSoloRoundUseCase;

    @Autowired
    private SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;

    @Autowired
    private EndRoadViewSoloRoundUseCase endRoadViewSoloRoundUseCase;

    @Autowired
    private CheckAndCompleteRoundEarlyUseCase checkAndCompleteRoundEarlyUseCase;

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

    @MockitoBean
    private GameRoomRedisAdaptor gameRoomRedisAdaptor;

    @Autowired
    private EntityManager entityManager;

    private Member hostMember;
    private List<Member> players;
    private GameRoom gameRoom;
    private Image markerImage;

    @BeforeEach
//    @Transactional  // ✅ 트랜잭션 추가
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
    @DisplayName("[통합] 모든 플레이어가 제출하면 라운드가 자동으로 조기 종료된다")
//    @Transactional
    void whenAllPlayersSubmit_thenRoundCompletesEarly() throws InterruptedException {
        // Given: 게임 시작
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), 60);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long gameId = startResponse.getGameId();
        Long roundId = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        log.info("🎮 게임 시작 - GameId: {}, RoundId: {}", gameId, roundId);

        // Redis 초기화
        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        entityManager.clear();
        // 라운드 상태 확인
        RoadViewGameRound round = roadViewGameRoundRepository.findById(roundId).orElseThrow();
        assertThat(round.getIsFinished()).isFalse();
        log.info("📍 라운드 초기 상태 확인 - 종료여부: {}", round.getIsFinished());

        // When: 모든 플레이어가 순차적으로 제출 (마지막 제출 시 조기 종료 트리거)
        List<GamePlayer> gamePlayers = gamePlayerRepository.findAllByMultiRoadViewGameId(gameId);
        assertThat(gamePlayers).hasSize(4);

        for (int i = 0; i < gamePlayers.size(); i++) {
            GamePlayer gamePlayer = gamePlayers.get(i);
            Member member = memberRepository.findById(gamePlayer.getMemberId()).orElseThrow();

            SubmitRoadViewRequest.Player submitRequest = SubmitRoadViewRequest.Player.builder()
                    .lat(37.5665 + (i * 0.01)) // 각기 다른 위치
                    .lng(126.9780 + (i * 0.01))
                    .timeToAnswer(5000.0 + (i * 1000.0))
                    .build();

            submitRoadViewPlayerAnswerUseCase.execute(member, roomId, gameId, roundId, submitRequest);

            long currentCount = submissionRedisService.getCurrentSubmissionCount(GameMode.ROADVIEW, roundId);
            log.info("📝 제출 완료 ({}/4) - PlayerId: {}, Distance: {}m, Count: {}", 
                    i + 1, gamePlayer.getId(), currentCount);

            Thread.sleep(100); // 이벤트 처리 대기
        }

        // 조기 종료 이벤트 처리 대기 (비동기)
        Thread.sleep(1000);

        // Then: 라운드가 자동으로 종료되었는지 확인
        round = roadViewGameRoundRepository.findById(roundId).orElseThrow();
        assertThat(round.getIsFinished()).isTrue();
        log.info("✅ 라운드 조기 종료 확인 - 종료여부: {}", round.getIsFinished());

        // 모든 제출 저장 확인
        List<RoadViewSubmission> submissions = submissionRepository.findSoloSubmissionsByRoundIdOrderByDistance(roundId);
        assertThat(submissions).hasSize(4);
        log.info("📊 제출 데이터 수: {}", submissions.size());

        // 라운드 결과 조회
        RoadViewRoundResponse.PlayerResult result = endRoadViewSoloRoundUseCase.execute(gameId, roundId);
        assertThat(result).isNotNull();
        assertThat(result.getPlayerSubmissionResults()).hasSize(4);
        log.info("🏆 라운드 결과 조회 완료 - 참가자 수: {}", result.getPlayerSubmissionResults().size());

        // 점수 확인 (거리 순으로 정렬되어야 함)
        assertThat(result.getPlayerSubmissionResults().get(0).getEarnedScore()).isGreaterThan(0);
        log.info("🥇 1등 - 거리: {}m, 점수: {}", 
                result.getPlayerSubmissionResults().get(0).getDistance(),
                result.getPlayerSubmissionResults().get(0).getEarnedScore());
        
        // 거리 순으로 정렬되었는지 확인
        for (int i = 1; i < result.getPlayerSubmissionResults().size(); i++) {
            assertThat(result.getPlayerSubmissionResults().get(i).getDistance())
                    .isGreaterThanOrEqualTo(result.getPlayerSubmissionResults().get(i - 1).getDistance());
        }
    }

    @Test
    @DisplayName("[통합] 일부 플레이어만 제출한 경우 조기 종료되지 않는다")
//    @Transactional
    void whenNotAllPlayersSubmit_thenRoundDoesNotComplete() throws InterruptedException {
        // Given: 게임 시작
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), 60);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long gameId = startResponse.getGameId();
        Long roundId = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        // When: 4명 중 2명만 제출
        List<GamePlayer> gamePlayers = gamePlayerRepository.findAllByMultiRoadViewGameId(gameId);
        for (int i = 0; i < 2; i++) {
            GamePlayer gamePlayer = gamePlayers.get(i);
            Member member = memberRepository.findById(gamePlayer.getMemberId()).orElseThrow();

            SubmitRoadViewRequest.Player submitRequest = SubmitRoadViewRequest.Player.builder()
                    .lat(37.5665)
                    .lng(126.9780)
                    .timeToAnswer(5000.0)
                    .build();

            submitRoadViewPlayerAnswerUseCase.execute(member, roomId, gameId, roundId, submitRequest);
        }

        Thread.sleep(500); // 이벤트 처리 대기

        // Then: 라운드가 종료되지 않음
        RoadViewGameRound round = roadViewGameRoundRepository.findById(roundId).orElseThrow();
        assertThat(round.getIsFinished()).isFalse();
        log.info("✅ 일부 제출 - 라운드 진행 중 (2/4 제출, 종료여부: {})", round.getIsFinished());

        // 제출 수 확인
        long submissionCount = submissionRedisService.getCurrentSubmissionCount(GameMode.ROADVIEW, roundId);
        assertThat(submissionCount).isEqualTo(2);
    }

    @Test
    @DisplayName("[통합] 개인전에서 플레이어 수에 따라 올바르게 조기 종료 판단한다")
//    @Transactional
    void earlyCompletion_BasedOnPlayerCount_NotTeamCount() throws InterruptedException {
        // Given: 게임 시작
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), 60);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long gameId = startResponse.getGameId();
        Long roundId = startResponse.getRoundInfo().getRoundId();

        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        // When: CheckAndCompleteRoundEarlyUseCase 직접 호출 (수동 검증)
        List<GamePlayer> gamePlayers = gamePlayerRepository.findAllByMultiRoadViewGameId(gameId);
        
        // 4명 중 3명만 제출한 경우
        for (int i = 0; i < 3; i++) {
            submissionRedisService.recordPlayerSubmission(
                    GameMode.ROADVIEW, 
                    roundId, 
                    gamePlayers.get(i).getId()
            );
        }

        boolean completed = checkAndCompleteRoundEarlyUseCase.execute(
                gameRoom.getId().toString(),
                gameId,
                roundId,
                GameMode.ROADVIEW,
                PlayerMatchType.SOLO
        );

        // Then: 4명 중 3명이므로 종료되지 않음
        assertThat(completed).isFalse();
        log.info("✅ 개인전 조기 종료 판단 - 3/4 제출, 결과: {}", completed);

        // 마지막 1명 제출
        submissionRedisService.recordPlayerSubmission(
                GameMode.ROADVIEW, 
                roundId, 
                gamePlayers.get(3).getId()
        );

        // DB에도 제출 데이터 저장 (실제 시나리오 시뮬레이션)
        RoadViewGameRound round = roadViewGameRoundRepository.findById(roundId).orElseThrow();
        for (GamePlayer gamePlayer : gamePlayers) {
            RoadViewSubmission submission = RoadViewSubmission.forPlayer(
                    gamePlayer, round, 37.5665, 126.9780, 5000.0
            );
            submission.setRound(round);
            submissionRepository.save(submission);
        }

        completed = checkAndCompleteRoundEarlyUseCase.execute(
                gameRoom.getId().toString(),
                gameId,
                roundId,
                GameMode.ROADVIEW,
                PlayerMatchType.SOLO
        );

        // 모두 제출했으므로 종료
        assertThat(completed).isTrue();
        log.info("✅ 개인전 조기 종료 판단 - 4/4 제출, 결과: {}", completed);
    }

    @Test
    @DisplayName("[통합] 중복 제출은 카운트되지 않는다")
    //@Transactional
    void duplicateSubmission_DoesNotCount() throws InterruptedException {
        // Given: 게임 시작
        MultiGameRequest.Start startRequest = createStartRequest(gameRoom.getId(), 60);
        MultiRoadViewGameResponse.StartPlayerGame startResponse = 
                startRoadViewSoloRoundUseCase.execute(hostMember, startRequest);

        Long gameId = startResponse.getGameId();
        Long roundId = startResponse.getRoundInfo().getRoundId();
        String roomId = gameRoom.getId().toString();

        submissionRedisService.initializeRound(GameMode.ROADVIEW, roundId);

        // When: 같은 플레이어가 두 번 제출 시도
        GamePlayer player1 = gamePlayerRepository.findAllByMultiRoadViewGameId(gameId).get(0);
        Member member1 = memberRepository.findById(player1.getMemberId()).orElseThrow();

        SubmitRoadViewRequest.Player submitRequest = SubmitRoadViewRequest.Player.builder()
                .lat(37.5665)
                .lng(126.9780)
                .timeToAnswer(5000.0)
                .build();

        // 첫 번째 제출
        submitRoadViewPlayerAnswerUseCase.execute(member1, roomId, gameId, roundId, submitRequest);
        long count1 = submissionRedisService.getCurrentSubmissionCount(GameMode.ROADVIEW, roundId);
        assertThat(count1).isEqualTo(1);

        // 두 번째 제출 시도 (예외 발생 예상)
        try {
            submitRoadViewPlayerAnswerUseCase.execute(member1, roomId, gameId, roundId, submitRequest);
        } catch (Exception e) {
            log.info("⚠️ 예상된 중복 제출 예외 발생: {}", e.getMessage());
        }

        // Then: 여전히 1개만 카운트
        long count2 = submissionRedisService.getCurrentSubmissionCount(GameMode.ROADVIEW, roundId);
        assertThat(count2).isEqualTo(1);
        log.info("✅ 중복 제출 방지 확인 - 제출 수: {}", count2);
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
                .title("조기 종료 테스트 방")
                .build();

        GameRoom savedRoom = gameRoomRepository.save(room);

        // 플레이어 참여 처리
        for (Member member : members) {
            savedRoom.join(member, null);
        }
        memberRepository.saveAll(members);
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

