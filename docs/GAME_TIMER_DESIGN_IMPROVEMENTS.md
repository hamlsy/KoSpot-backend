# 🎯 게임 타이머 시스템 개선 제안서

## 📌 개요
15년차 백엔드 엔지니어 관점에서 현재 게임 타이머 시스템의 개선 사항을 정리합니다.
클린 아키텍처와 DDD 원칙을 기반으로 더 나은 구조와 설계를 제안합니다.

---

## 1️⃣ TimeLimit 저장 위치 개선

### 현재 문제점
- `GameRound`마다 `timeLimit`를 중복 저장
- 데이터 일관성 보장이 어려움
- 라운드 생성 시마다 timeLimit을 전달해야 함

### 개선 방안: Game 레벨로 이동

```java
// 개선된 MultiGame 엔티티
@MappedSuperclass
public abstract class MultiGame extends BaseTimeEntity {
    
    // 기존 필드...
    @Min(10)
    @Max(180)
    private Integer timeLimit; // 초 단위, Game 레벨에서 관리
    
    /**
     * 타임 리밋을 Duration으로 반환
     * null이면 GameMode의 기본값 사용
     */
    public Duration getTimeLimit() {
        if (timeLimit != null) {
            return Duration.ofSeconds(timeLimit);
        }
        return gameMode.getDuration();
    }
    
    /**
     * 타임 리밋 설정 (빌더 외 직접 설정 방지)
     */
    protected void setTimeLimit(Integer timeLimit) {
        validateTimeLimit(timeLimit);
        this.timeLimit = timeLimit;
    }
    
    private void validateTimeLimit(Integer timeLimit) {
        if (timeLimit != null && (timeLimit < 10 || timeLimit > 180)) {
            throw new GameHandler(ErrorStatus.INVALID_TIME_LIMIT);
        }
    }
}
```

```java
// 개선된 BaseGameRound
@MappedSuperclass
public abstract class BaseGameRound extends BaseTimeEntity {
    
    // timeLimit 필드 제거!
    
    private Instant serverStartTime;
    
    /**
     * 상위 Game 엔티티를 반환하는 추상 메서드
     * 각 구체 클래스에서 구현
     */
    public abstract MultiGame getGame();
    
    /**
     * Game에서 timeLimit 가져오기
     */
    public Duration getDuration() {
        return getGame().getTimeLimit();
    }
    
    public long getRemainingTimeMs() {
        if (this.serverStartTime == null) {
            return getDuration().toMillis();
        }
        Duration elapsed = Duration.between(this.serverStartTime, Instant.now());
        long remaining = getDuration().toMillis() - elapsed.toMillis();
        return Math.max(remaining, 0);
    }
}
```

```java
// RoadViewGameRound 구현
@Entity
public class RoadViewGameRound extends BaseGameRound {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_road_view_game_id")
    private MultiRoadViewGame multiRoadViewGame;
    
    @Override
    public MultiGame getGame() {
        return this.multiRoadViewGame;
    }
    
    // 생성 메서드에서 timeLimit 파라미터 제거
    public static RoadViewGameRound createRound(
            Integer roundNumber,
            CoordinateNationwide targetCoordinate,
            List<Long> playerIds) { // timeLimit 제거!
        return RoadViewGameRound.builder()
                .roundNumber(roundNumber)
                .targetCoordinate(targetCoordinate)
                .playerIds(playerIds)
                .isFinished(false)
                .build();
    }
}
```

### 사용 예시 개선

```java
// Before: timeLimit을 매번 전달
roundService.createGameRound(game, 1, request.getTimeLimit(), playerIds);
roundService.createGameRound(game, 2, request.getTimeLimit(), playerIds);

// After: Game에서 자동으로 가져옴
roundService.createGameRound(game, 1, playerIds);
roundService.createGameRound(game, 2, playerIds);
```

---

## 2️⃣ Controller RequestMapping 개선

### 현재 문제점
```java
// 계층 구조가 명확하지 않음
@PostMapping("/{multiGameId}/rounds/{roundId}/endPlayerRound")
@PostMapping("/{multiGameId}/rounds/nextRound/next") // nextRound 중복
```

### 개선된 Controller 설계

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/game-rooms/{gameRoomId}/roadview")
@Tag(name = "Multi RoadView Game API", description = "멀티 로드뷰 게임 API")
public class MultiRoadViewGameController {

    private final StartRoadViewSoloRoundUseCase startRoadViewSoloRoundUseCase;
    private final SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;
    private final NextRoadViewRoundUseCase nextRoadViewRoundUseCase;
    private final EndRoadViewSoloRoundUseCase endRoadViewSoloRoundUseCase;

    @PostMapping("/games/start")
    @Operation(summary = "멀티 로드뷰 게임 시작")
    public ApiResponseDto<MultiRoadViewGameResponse.StartPlayerGame> startGame(
            @PathVariable("gameRoomId") Long gameRoomId,
            @CurrentMember Member member,
            @RequestBody @Valid MultiGameRequest.Start request) {
        
        // Validation: request의 gameRoomId와 경로의 gameRoomId 일치 확인
        validateGameRoomId(gameRoomId, request.getGameRoomId());
        
        return ApiResponseDto.onSuccess(
            startRoadViewSoloRoundUseCase.execute(member, request)
        );
    }

    @PostMapping("/games/{gameId}/rounds/{roundId}/submissions")
    @Operation(summary = "멀티 로드뷰 정답 제출")
    public ApiResponseDto<Void> submitAnswer(
            @PathVariable("gameRoomId") Long gameRoomId,
            @PathVariable("gameId") Long gameId,
            @PathVariable("roundId") Long roundId,
            @RequestBody @Valid SubmissionRequest.RoadViewPlayer request) {
        
        submitRoadViewPlayerAnswerUseCase.execute(
            gameRoomId, gameId, roundId, request
        );
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @PostMapping("/games/{gameId}/rounds/{roundId}/end")
    @Operation(summary = "멀티 로드뷰 라운드 종료")
    public ApiResponseDto<RoadViewRoundResponse.PlayerResult> endRound(
            @PathVariable("gameRoomId") Long gameRoomId,
            @PathVariable("gameId") Long gameId,
            @PathVariable("roundId") Long roundId) {
        
        return ApiResponseDto.onSuccess(
            endRoadViewSoloRoundUseCase.execute(gameRoomId, gameId, roundId)
        );
    }

    @PostMapping("/games/{gameId}/rounds/next")
    @Operation(summary = "멀티 로드뷰 다음 라운드 시작")
    public ApiResponseDto<MultiRoadViewGameResponse.NextRound> nextRound(
            @PathVariable("gameRoomId") Long gameRoomId,
            @PathVariable("gameId") Long gameId) {
        
        return ApiResponseDto.onSuccess(
            nextRoadViewRoundUseCase.execute(gameRoomId, gameId)
        );
    }

    private void validateGameRoomId(Long pathGameRoomId, Long requestGameRoomId) {
        if (!pathGameRoomId.equals(requestGameRoomId)) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_ID_MISMATCH);
        }
    }
}
```

### RESTful API 구조

```
GameRoom (1) ──< (N) Game (1) ──< (N) Round

REST API 경로:
/api/v1/game-rooms/{gameRoomId}/roadview/games/start
/api/v1/game-rooms/{gameRoomId}/roadview/games/{gameId}/rounds/{roundId}/submissions
/api/v1/game-rooms/{gameRoomId}/roadview/games/{gameId}/rounds/{roundId}/end
/api/v1/game-rooms/{gameRoomId}/roadview/games/{gameId}/rounds/next

WebSocket 채널:
/topic/game-rooms/{gameRoomId}/timer/start
/topic/game-rooms/{gameRoomId}/timer/sync
/topic/game-rooms/{gameRoomId}/rounds/{roundId}/results
```

---

## 3️⃣ UseCase 계층 개선

### 현재 문제점
- UseCase가 너무 많은 책임을 가짐
- 도메인 로직이 UseCase에 유출됨
- 트랜잭션 경계가 명확하지 않음

### 개선된 StartRoadViewSoloRoundUseCase

```java
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class StartRoadViewSoloRoundUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final MultiRoadViewGameService multiRoadViewGameService;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerService gamePlayerService;
    private final GameTimerService gameTimerService;
    private final GameEventPublisher gameEventPublisher; // 추가

    /**
     * 멀티 로드뷰 게임 시작
     * 
     * 처리 순서:
     * 1. GameRoom 검증 및 상태 변경
     * 2. MultiGame 생성
     * 3. GamePlayer 생성
     * 4. 첫 번째 Round 생성
     * 5. Timer 시작
     * 6. 이벤트 발행
     */
    public MultiRoadViewGameResponse.StartPlayerGame execute(
            Member host, 
            MultiGameRequest.Start request) {
        
        // 1. GameRoom 조회 및 검증
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchHost(request.getGameRoomId());
        gameRoom.start(host); // 도메인 로직으로 위임
        
        // 2. 게임 생성 및 시작
        MultiRoadViewGame game = multiRoadViewGameService.createGame(
            gameRoom, 
            request.getPlayerMatchTypeKey(),
            request.getTotalRounds(),
            request.getTimeLimit() // Game 레벨에 저장
        );
        game.startGame();
        
        // 3. 플레이어 생성
        List<GamePlayer> gamePlayers = gamePlayerService.createRoadViewGamePlayers(
            gameRoom, 
            game
        );
        List<Long> playerIds = gamePlayers.stream()
                .map(GamePlayer::getId)
                .toList();
        
        // 4. 첫 번째 라운드 생성 (timeLimit은 Game에서 자동 참조)
        RoadViewGameRound round = roadViewGameRoundService.createFirstRound(
            game, 
            playerIds
        );
        
        // 5. 타이머 시작
        gameTimerService.startRoundTimer(
            TimerCommand.of(gameRoom.getId(), game, round)
        );
        
        // 6. 게임 시작 이벤트 발행 (비동기 처리용)
        gameEventPublisher.publishGameStarted(
            new GameStartedEvent(game.getId(), gameRoom.getId(), playerIds)
        );
        
        log.info("🎮 게임 시작: gameId={}, roomId={}, players={}", 
                game.getId(), gameRoom.getId(), playerIds.size());
        
        return MultiRoadViewGameResponse.StartPlayerGame.from(
            game, 
            round, 
            gamePlayers
        );
    }
}
```

---

## 4️⃣ GameTimerService 개선

### 현재 문제점
- `Map<String, ScheduledFuture<?>>` 동시성 문제
- Task 관리가 산발적
- 에러 처리 부족

### 개선된 GameTimerService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class GameTimerService {

    private final GameRoundRedisRepository gameRoundRedisRepository;
    private final GameTimerRedisRepository gameTimerRedisRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskScheduler gameTimerTaskScheduler;

    // ConcurrentHashMap으로 명시적 선언
    private final ConcurrentHashMap<String, TimerTask> activeTasks = new ConcurrentHashMap<>();

    private static final int SYNC_INTERVAL_MS = 5000;
    private static final int FINAL_COUNTDOWN_THRESHOLD_MS = 10000;

    /**
     * 라운드 타이머 시작
     * - 서버 기준 시간으로 타이머 시작
     * - Redis에 타이머 정보 저장 (서버 재시작 대비)
     * - 주기적 동기화 스케줄링
     * - 라운드 종료 스케줄링
     */
    public void startRoundTimer(TimerCommand command) {
        String taskKey = generateTaskKey(command.getGameRoomId(), command.getRound().getRoundId());
        
        // 기존 타이머가 있으면 먼저 정리
        stopExistingTimer(taskKey);
        
        try {
            BaseGameRound round = command.getRound();
            Instant serverStartTime = Instant.now();
            
            // 1. 시작 메시지 브로드캐스트
            broadcastTimerStart(command, serverStartTime);
            
            // 2. Redis에 타이머 정보 저장 (장애 복구용)
            saveTimerToRedis(command, serverStartTime);
            
            // 3. 동기화 및 종료 스케줄링
            TimerTask timerTask = scheduleTimerTasks(command, serverStartTime);
            activeTasks.put(taskKey, timerTask);
            
            log.info("⏰ 타이머 시작: gameRoomId={}, roundId={}, duration={}초", 
                    command.getGameRoomId(), 
                    round.getRoundId(), 
                    round.getDuration().getSeconds());
            
        } catch (Exception e) {
            log.error("타이머 시작 실패: {}", taskKey, e);
            // 실패한 태스크 정리
            stopExistingTimer(taskKey);
            throw new TimerException("타이머 시작에 실패했습니다", e);
        }
    }

    /**
     * 라운드 타이머 수동 중지
     */
    public void stopRoundTimer(String gameRoomId, String roundId) {
        String taskKey = generateTaskKey(gameRoomId, roundId);
        stopExistingTimer(taskKey);
        
        // Redis에서도 제거
        gameTimerRedisRepository.deleteTimer(gameRoomId, roundId);
        
        log.info("🛑 타이머 중지: gameRoomId={}, roundId={}", gameRoomId, roundId);
    }

    // === Private Methods ===

    private void broadcastTimerStart(TimerCommand command, Instant serverStartTime) {
        BaseGameRound round = command.getRound();
        
        TimerStartMessage startMessage = TimerStartMessage.builder()
                .roundId(round.getRoundId())
                .gameMode(round.getGameMode())
                .serverStartTimeMs(serverStartTime.toEpochMilli())
                .durationMs(round.getDuration().toMillis())
                .serverTimestamp(System.currentTimeMillis())
                .build();

        String channel = MultiGameChannelConstants.getTimerStartChannel(
            command.getGameRoomId()
        );
        
        messagingTemplate.convertAndSend(channel, startMessage);
        log.debug("📡 타이머 시작 브로드캐스트: {}", channel);
    }

    private TimerTask scheduleTimerTasks(TimerCommand command, Instant serverStartTime) {
        BaseGameRound round = command.getRound();
        String gameRoomId = command.getGameRoomId();
        
        // 동기화 스케줄링 (5초 간격)
        ScheduledFuture<?> syncFuture = scheduleSyncTask(gameRoomId, round);
        
        // 종료 스케줄링
        ScheduledFuture<?> completionFuture = scheduleCompletionTask(
            command, 
            serverStartTime
        );
        
        return new TimerTask(syncFuture, completionFuture);
    }

    private ScheduledFuture<?> scheduleSyncTask(String gameRoomId, BaseGameRound round) {
        return gameTimerTaskScheduler.scheduleAtFixedRate(
            () -> broadcastTimerSync(gameRoomId, round),
            Instant.now().plusMillis(SYNC_INTERVAL_MS),
            Duration.ofMillis(SYNC_INTERVAL_MS)
        );
    }

    private void broadcastTimerSync(String gameRoomId, BaseGameRound round) {
        try {
            long remainingTimeMs = round.getRemainingTimeMs();
            
            // 타이머 종료 시 동기화 중지
            if (remainingTimeMs <= 0) {
                stopRoundTimer(gameRoomId, round.getRoundId());
                return;
            }
            
            boolean isFinalCountdown = remainingTimeMs <= FINAL_COUNTDOWN_THRESHOLD_MS;
            
            TimerSyncMessage syncMessage = TimerSyncMessage.builder()
                    .roundId(round.getRoundId())
                    .remainingTimeMs(remainingTimeMs)
                    .serverTimestamp(System.currentTimeMillis())
                    .isFinalCountDown(isFinalCountdown)
                    .build();
            
            String channel = MultiGameChannelConstants.getTimerSyncChannel(gameRoomId);
            messagingTemplate.convertAndSend(channel, syncMessage);
            
            log.debug("🔄 타이머 동기화: roundId={}, 남은시간={}초", 
                    round.getRoundId(), 
                    remainingTimeMs / 1000);
            
        } catch (Exception e) {
            log.error("타이머 동기화 실패: gameRoomId={}, roundId={}", 
                    gameRoomId, round.getRoundId(), e);
        }
    }

    private ScheduledFuture<?> scheduleCompletionTask(
            TimerCommand command, 
            Instant serverStartTime) {
        
        BaseGameRound round = command.getRound();
        Instant completionTime = serverStartTime.plus(round.getDuration());
        
        return gameTimerTaskScheduler.schedule(
            () -> handleRoundCompletion(command),
            completionTime
        );
    }

    private void handleRoundCompletion(TimerCommand command) {
        String gameRoomId = command.getGameRoomId();
        BaseGameRound round = command.getRound();
        
        try {
            log.info("⏱️ 라운드 종료: gameRoomId={}, roundId={}", 
                    gameRoomId, round.getRoundId());
            
            // 라운드 종료 이벤트 발행
            RoundCompletionEvent event = new RoundCompletionEvent(
                gameRoomId,
                round.getRoundId(),
                command.getGameMode(),
                command.getMatchType(),
                command.getGameId()
            );
            eventPublisher.publishEvent(event);
            
            // Task 정리
            String taskKey = generateTaskKey(gameRoomId, round.getRoundId());
            stopExistingTimer(taskKey);
            
        } catch (Exception e) {
            log.error("라운드 종료 처리 실패: gameRoomId={}, roundId={}", 
                    gameRoomId, round.getRoundId(), e);
        }
    }

    private void saveTimerToRedis(TimerCommand command, Instant serverStartTime) {
        BaseGameRound round = command.getRound();
        
        gameTimerRedisRepository.saveRound(
            command.getGameRoomId(),
            round,
            round.getRoundId(),
            round.getDuration(),
            command.getRound().getPlayerIds().stream()
                    .map(String::valueOf)
                    .toList(),
            serverStartTime
        );
    }

    private void stopExistingTimer(String taskKey) {
        TimerTask task = activeTasks.remove(taskKey);
        if (task != null) {
            task.cancel();
        }
    }

    private String generateTaskKey(String gameRoomId, String roundId) {
        return String.format("%s:%s", gameRoomId, roundId);
    }

    /**
     * 타이머 태스크를 관리하는 내부 클래스
     */
    @RequiredArgsConstructor
    private static class TimerTask {
        private final ScheduledFuture<?> syncFuture;
        private final ScheduledFuture<?> completionFuture;

        public void cancel() {
            if (syncFuture != null && !syncFuture.isCancelled()) {
                syncFuture.cancel(false);
            }
            if (completionFuture != null && !completionFuture.isCancelled()) {
                completionFuture.cancel(false);
            }
        }
    }
}
```

---

## 5️⃣ 도메인 이벤트 패턴 도입

### 이벤트 기반 설계의 장점
- UseCase의 책임 분리
- 비즈니스 로직의 느슨한 결합
- 확장성 향상 (이벤트 리스너 추가로 기능 확장)

### 이벤트 정의

```java
@Getter
@AllArgsConstructor
public class GameStartedEvent {
    private final Long gameId;
    private final Long gameRoomId;
    private final List<Long> playerIds;
    private final Instant occurredAt = Instant.now();
}

@Getter
@AllArgsConstructor
public class RoundCompletionEvent {
    private final String gameRoomId;
    private final String roundId;
    private final GameMode gameMode;
    private final PlayerMatchType matchType;
    private final String gameId;
    private final Instant occurredAt = Instant.now();
}
```

### 이벤트 리스너

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class GameEventListener {

    private final GameRoomNotificationService notificationService;
    private final GameStatisticsService statisticsService;

    @EventListener
    @Async("gameEventExecutor")
    public void handleGameStarted(GameStartedEvent event) {
        log.info("게임 시작 이벤트 처리: gameId={}", event.getGameId());
        
        // 플레이어들에게 알림 전송
        notificationService.notifyGameStarted(
            event.getGameRoomId(), 
            event.getPlayerIds()
        );
        
        // 통계 기록
        statisticsService.recordGameStart(
            event.getGameId(), 
            event.getPlayerIds().size()
        );
    }

    @EventListener
    @Async("gameEventExecutor")
    public void handleRoundCompletion(RoundCompletionEvent event) {
        log.info("라운드 종료 이벤트 처리: roundId={}", event.getRoundId());
        
        // 결과 집계 및 브로드캐스트
        notificationService.broadcastRoundResults(
            event.getGameRoomId(), 
            event.getRoundId()
        );
    }
}
```

---

## 6️⃣ 에러 처리 및 복구 전략

### TimerException 정의

```java
public class TimerException extends RuntimeException {
    public TimerException(String message) {
        super(message);
    }
    
    public TimerException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 장애 복구 로직

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class GameTimerRecoveryService {

    private final GameTimerRedisRepository gameTimerRedisRepository;
    private final GameTimerService gameTimerService;

    /**
     * 서버 재시작 시 Redis에서 활성 타이머 복구
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverActiveTimers() {
        log.info("🔄 활성 타이머 복구 시작");
        
        List<String> activeGameRoomIds = gameTimerRedisRepository.findAllActiveGameRooms();
        
        for (String gameRoomId : activeGameRoomIds) {
            try {
                recoverGameRoomTimers(gameRoomId);
            } catch (Exception e) {
                log.error("타이머 복구 실패: gameRoomId={}", gameRoomId, e);
            }
        }
        
        log.info("✅ 타이머 복구 완료: {} 개 게임룸", activeGameRoomIds.size());
    }

    private void recoverGameRoomTimers(String gameRoomId) {
        List<BaseGameRound> activeRounds = gameTimerRedisRepository
                .findActiveRounds(gameRoomId);
        
        for (BaseGameRound round : activeRounds) {
            if (round.isTimeExpired()) {
                // 이미 만료된 라운드는 종료 처리
                handleExpiredRound(gameRoomId, round);
            } else {
                // 타이머 재시작
                restartTimer(gameRoomId, round);
            }
        }
    }
}
```

---

## 7️⃣ 테스트 전략

### 테스트 계층

1. **단위 테스트** (`GameTimerServiceTest.java`)
   - Mock을 사용한 서비스 로직 검증
   - 타이머 계산 정확성 검증
   - 에러 처리 검증

2. **통합 테스트** (`GameTimerIntegrationTest.java`)
   - 실제 Spring Context 사용
   - 실제 TaskScheduler로 스케줄링 검증
   - 5초 간격 브로드캐스팅 검증

3. **성능 테스트**
   - 100개 동시 게임룸의 타이머 처리
   - 메모리 누수 검증
   - Task 정리 확인

---

## 8️⃣ 모니터링 및 관측성

### 메트릭 추가

```java
@Component
@RequiredArgsConstructor
public class GameTimerMetrics {

    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        // 활성 타이머 수
        Gauge.builder("game.timer.active", activeTasks, Map::size)
                .description("현재 활성 타이머 수")
                .register(meterRegistry);
    }

    public void recordTimerStart() {
        meterRegistry.counter("game.timer.started").increment();
    }

    public void recordTimerCompletion() {
        meterRegistry.counter("game.timer.completed").increment();
    }

    public void recordTimerError() {
        meterRegistry.counter("game.timer.errors").increment();
    }
}
```

---

## 📊 개선 전후 비교

| 항목 | 개선 전 | 개선 후 |
|------|---------|---------|
| **TimeLimit 저장** | Round마다 중복 저장 | Game에 한 번만 저장 |
| **데이터 일관성** | Round별로 다를 수 있음 | Game 레벨에서 보장 |
| **API 경로** | 계층 구조 불명확 | RESTful 계층 구조 |
| **동시성** | Map 타입 불명확 | ConcurrentHashMap 명시 |
| **에러 처리** | 부족 | Try-catch 및 복구 로직 |
| **테스트** | 부족 | 단위/통합 테스트 완비 |
| **관측성** | 로그만 존재 | 메트릭 및 이벤트 |

---

## 🎯 결론

1. **TimeLimit는 Game 레벨에 저장**하여 단일 진실 공급원(Single Source of Truth) 원칙 준수
2. **RESTful API 계층 구조**를 명확히 하여 유지보수성 향상
3. **이벤트 기반 설계**로 UseCase의 책임 분리 및 확장성 확보
4. **철저한 테스트 코드**로 타이머 정확성 보장
5. **에러 처리 및 복구 전략**으로 장애 대응력 강화

이러한 개선을 통해 클린 아키텍처와 DDD 원칙을 준수하면서도 
실전에서 안정적으로 동작하는 게임 타이머 시스템을 구축할 수 있습니다.

