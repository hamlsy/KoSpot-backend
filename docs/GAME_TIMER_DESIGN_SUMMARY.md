# 🎯 게임 타이머 설계 개선 최종 요약

## 📌 질문별 답변 요약

### 1️⃣ TimeLimit 저장 위치: Game vs GameRound

**✅ 결론: Game 엔티티에 저장하는 것이 더 나은 설계입니다.**

#### 이유:

**A. 단일 진실 공급원 (Single Source of Truth)**
- 게임 시작 시 결정된 timeLimit는 모든 라운드에 동일하게 적용
- Game이 게임 규칙(matchType, totalRounds, timeLimit)을 소유
- Round는 Game의 규칙을 참조하는 구조

**B. 데이터 일관성 보장**
- 각 Round마다 timeLimit를 저장하면 일관성 보장이 어려움
- 실수로 다른 timeLimit 값이 들어갈 가능성 제거

**C. 메모리 및 스토리지 효율성**
- 15라운드 게임: 동일한 값을 15번 저장 vs 1번 저장
- DB 정규화 관점에서도 중복 제거

**D. 코드 간결성**
```java
// Before: timeLimit을 매번 전달
roundService.createGameRound(game, 1, request.getTimeLimit(), playerIds);
roundService.createGameRound(game, 2, request.getTimeLimit(), playerIds);

// After: Game에서 자동으로 참조
roundService.createGameRound(game, 1, playerIds);
roundService.createGameRound(game, 2, playerIds);
```

**E. DDD 관점**
- Game이 애그리게이트 루트
- Round는 Game의 일부 엔티티
- 게임 규칙은 애그리게이트 루트가 관리

#### 예외 상황:
만약 **라운드마다 다른 timeLimit**를 허용하는 게임 모드가 있다면 (예: 난이도 증가),  
현재 설계가 더 유연할 수 있습니다. 하지만 귀하의 요구사항은 "게임 시작 시 timeLimit 결정"이므로  
**Game 레벨 저장이 적합합니다.**

---

### 2️⃣ Controller RequestMapping 설계

**✅ 결론: gameRoomId를 경로에 포함하는 것이 바람직합니다.**

#### 권장 구조:

```
계층 구조:
GameRoom (1) ──< (N) Game (1) ──< (N) Round

REST API 경로:
/api/v1/game-rooms/{gameRoomId}/roadview/games/start
/api/v1/game-rooms/{gameRoomId}/roadview/games/{gameId}/rounds/{roundId}/submissions
/api/v1/game-rooms/{gameRoomId}/roadview/games/{gameId}/rounds/{roundId}/end
/api/v1/game-rooms/{gameRoomId}/roadview/games/{gameId}/rounds/next

WebSocket 채널 (일관성):
/topic/game-rooms/{gameRoomId}/timer/start
/topic/game-rooms/{gameRoomId}/timer/sync
```

#### 이유:

**A. RESTful API 모범 사례**
- URL은 리소스의 계층 구조를 나타내야 함
- 부모-자식 관계가 명확함

**B. WebSocket 채널과의 일관성**
- REST API와 WebSocket 채널이 동일한 구조 사용
- 클라이언트 개발자가 이해하기 쉬움

**C. 보안 및 검증 강화**
```java
// gameId가 실제로 이 gameRoomId에 속하는지 검증 가능
public void validateAccess(Long gameRoomId, Long gameId, Member member) {
    Game game = gameRepository.findById(gameId);
    if (!game.getGameRoom().getId().equals(gameRoomId)) {
        throw new InvalidAccessException();
    }
}
```

**D. 명확한 컨텍스트**
- 어떤 게임 방에서 어떤 게임을 진행 중인지 명확
- 디버깅 및 로깅 시 유용

---

### 3️⃣ 게임 타이머 테스트 코드

**✅ 작성 완료:**
- `src/test/java/com/kospot/multi/timer/GameTimerServiceTest.java` (단위 테스트)
- `src/test/java/com/kospot/multi/timer/GameTimerIntegrationTest.java` (통합 테스트)

#### 테스트 커버리지:

**단위 테스트:**
1. 타이머 시작 시 모든 클라이언트에게 동일한 서버 시작 시간 브로드캐스팅
2. 5초 간격 동기화 스케줄링 검증
3. 남은 시간 계산 정확성 검증
4. 타이머 종료 시점 스케줄링 검증
5. 여러 클라이언트 동시 접속 시나리오
6. 타이머 수동 중지 시 스케줄링 취소 검증
7. 마지막 10초 카운트다운 플래그 검증

**통합 테스트:**
1. 실제 5초 간격 브로드캐스팅 검증
2. 모든 클라이언트가 동일한 서버 시작 시간 수신
3. 마지막 10초 임계값 도달 시 플래그 활성화
4. 타이머 중지 후 동기화 메시지 전송 중단
5. 서버 시간 기반 계산의 일관성

#### 핵심 검증 사항:
```java
// ✅ 모든 클라이언트가 동일한 서버 시작 시간 수신
assertThat(receivedStartTimes).allMatch(time -> time.equals(firstStartTime));

// ✅ 5초 간격 정확도 (±200ms 허용)
assertThat(interval).isBetween(4800L, 5200L);

// ✅ 남은 시간이 실제 경과 시간만큼 감소
assertThat(actualDecrease).isBetween(4500L, 5500L);
```

---

### 4️⃣ 추가 개선 사항

#### A. GameTimerService 개선

**현재 문제점:**
- `Map<String, ScheduledFuture<?>>` 동시성 문제 가능성
- 에러 처리 부족
- Task 관리가 산발적

**개선 사항:**
```java
// 1. ConcurrentHashMap 명시적 선언
private final ConcurrentHashMap<String, TimerTask> activeTasks = new ConcurrentHashMap<>();

// 2. TimerTask 내부 클래스로 관리
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

// 3. Try-catch로 에러 처리
try {
    // 타이머 시작 로직
} catch (Exception e) {
    log.error("타이머 시작 실패: {}", taskKey, e);
    stopExistingTimer(taskKey);
    throw new TimerException("타이머 시작에 실패했습니다", e);
}

// 4. Redis에 타이머 정보 저장 (장애 복구용)
saveTimerToRedis(command, serverStartTime);
```

#### B. 도메인 이벤트 패턴 도입

**장점:**
- UseCase의 책임 분리
- 비즈니스 로직의 느슨한 결합
- 확장성 향상

```java
// 이벤트 발행
gameEventPublisher.publishGameStarted(
    new GameStartedEvent(game.getId(), gameRoom.getId(), playerIds)
);

// 이벤트 리스너 (비동기)
@EventListener
@Async("gameEventExecutor")
public void handleGameStarted(GameStartedEvent event) {
    notificationService.notifyGameStarted(event.getGameRoomId(), event.getPlayerIds());
    statisticsService.recordGameStart(event.getGameId(), event.getPlayerIds().size());
}
```

#### C. 장애 복구 전략

**서버 재시작 시 타이머 복구:**
```java
@EventListener(ApplicationReadyEvent.class)
public void recoverActiveTimers() {
    log.info("🔄 활성 타이머 복구 시작");
    
    List<String> activeGameRoomIds = gameTimerRedisRepository.findAllActiveGameRooms();
    
    for (String gameRoomId : activeGameRoomIds) {
        List<BaseGameRound> activeRounds = gameTimerRedisRepository.findActiveRounds(gameRoomId);
        
        for (BaseGameRound round : activeRounds) {
            if (round.isTimeExpired()) {
                handleExpiredRound(gameRoomId, round);
            } else {
                restartTimer(gameRoomId, round);
            }
        }
    }
}
```

#### D. 모니터링 및 관측성

**메트릭 추가:**
```java
// 활성 타이머 수
Gauge.builder("game.timer.active", activeTasks, Map::size)
        .register(meterRegistry);

// 타이머 시작 횟수
meterRegistry.counter("game.timer.started").increment();

// 타이머 에러 횟수
meterRegistry.counter("game.timer.errors").increment();
```

#### E. 로깅 개선

**주요 이벤트에 상세 로그:**
```java
log.info("🎮 게임 시작: gameId={}, roomId={}, players={}", 
        game.getId(), gameRoom.getId(), playerIds.size());

log.info("⏰ 타이머 시작: gameRoomId={}, roundId={}, duration={}초", 
        command.getGameRoomId(), 
        round.getRoundId(), 
        round.getDuration().getSeconds());

log.info("⏱️ 라운드 종료: gameRoomId={}, roundId={}", 
        gameRoomId, round.getRoundId());
```

---

## 📊 개선 전후 비교 요약

| 항목 | 개선 전 | 개선 후 |
|------|---------|---------|
| **TimeLimit 저장** | Round마다 중복 저장 | Game에 한 번만 저장 |
| **데이터 일관성** | Round별로 다를 수 있음 | Game 레벨에서 보장 |
| **API 경로** | 계층 구조 불명확 | RESTful 계층 구조 |
| **동시성** | Map 타입 불명확 | ConcurrentHashMap 명시 |
| **에러 처리** | 부족 | Try-catch 및 복구 로직 |
| **테스트** | 부족 | 단위/통합 테스트 완비 |
| **관측성** | 로그만 존재 | 메트릭 및 이벤트 |
| **코드 간결성** | 중복 파라미터 전달 | 자동 참조로 간소화 |

---

## 🎓 신입 개발자를 위한 학습 포인트

### 1. 단일 진실 공급원 (Single Source of Truth)
- 동일한 데이터를 여러 곳에 저장하면 일관성 문제 발생
- 한 곳에서 관리하고 나머지는 참조하는 구조가 바람직

### 2. 애그리게이트 패턴 (DDD)
- Game이 애그리게이트 루트, Round는 일부 엔티티
- 게임 규칙은 애그리게이트 루트가 관리
- 엔티티 간 관계를 명확히 하면 책임 분리가 자연스러움

### 3. RESTful API 설계
- URL은 리소스의 계층 구조를 나타냄
- `/api/v1/game-rooms/{gameRoomId}/games/{gameId}/rounds/{roundId}`
- 부모-자식 관계가 명확하면 API 사용이 직관적

### 4. 동시성 관리
- 멀티스레드 환경에서는 `ConcurrentHashMap` 사용
- Task 관리 시 명시적인 cancel 메서드 제공
- 에러 발생 시 리소스 정리 필수

### 5. 이벤트 기반 설계
- UseCase는 핵심 비즈니스 로직에 집중
- 부가적인 작업(알림, 통계)은 이벤트로 분리
- 확장성과 유지보수성 향상

### 6. 테스트 전략
- 단위 테스트: Mock을 사용한 로직 검증
- 통합 테스트: 실제 환경에서 동작 검증
- 타이머 같은 시간 의존 로직은 철저한 테스트 필수

### 7. 관측성 (Observability)
- 로그: 주요 이벤트 기록
- 메트릭: 정량적 지표 수집
- 트레이싱: 요청 흐름 추적
- 장애 발생 시 빠른 원인 파악 가능

### 8. 장애 복구
- Redis에 타이머 정보 저장
- 서버 재시작 시 복구 로직 구현
- 예외 상황에 대한 대비책 마련

---

## 📝 다음 단계 권장 사항

### 1. 마이그레이션 계획
1. MultiGame 엔티티에 timeLimit 필드 추가
2. BaseGameRound에서 timeLimit 필드 제거
3. 데이터베이스 마이그레이션 스크립트 작성
4. 기존 데이터 이관 (Round의 timeLimit → Game)

### 2. API 버전 관리
- 기존 API는 `/api/v1/multiRoadView/...` 유지 (deprecated)
- 새 API는 `/api/v2/game-rooms/{gameRoomId}/roadview/...` 제공
- 점진적 마이그레이션 (6개월 후 v1 제거)

### 3. 테스트 실행
```bash
# 단위 테스트
./gradlew test --tests GameTimerServiceTest

# 통합 테스트
./gradlew test --tests GameTimerIntegrationTest
```

### 4. 성능 테스트
- 100개 동시 게임룸 시나리오
- 메모리 사용량 모니터링
- Task 누수 검증

### 5. 문서화
- API 명세서 업데이트 (Swagger)
- 아키텍처 다이어그램 작성
- 운영 가이드 작성

---

## 🎯 결론

1. **TimeLimit는 Game 레벨에 저장**하여 단일 진실 공급원 원칙 준수
2. **RESTful API 계층 구조**를 명확히 하여 유지보수성 향상
3. **철저한 테스트 코드**로 타이머 정확성 보장
4. **에러 처리 및 복구 전략**으로 장애 대응력 강화
5. **도메인 이벤트 패턴**으로 UseCase의 책임 분리 및 확장성 확보

이러한 개선을 통해 클린 아키텍처와 DDD 원칙을 준수하면서도  
실전에서 안정적으로 동작하는 게임 타이머 시스템을 구축할 수 있습니다.

---

**작성자:** AI Senior Backend Engineer (15년차)  
**작성일:** 2025-10-06  
**버전:** 1.0

