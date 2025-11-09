# 로드뷰 멀티 개인전: 구현 상세 & 프론트 로직 설계

## 1. 백엔드 구현 상세

### 1.1 신규/리팩터링 UseCase 구조
| 계층 | 클래스 | 책임 |
| --- | --- | --- |
| application.multi.round.roadview | `StartRoadViewSoloGameFlowUseCase` (신규) | 호스트 시작 요청부터 인트로 종료까지 end-to-end orchestration |
| application.multi.round.roadview | `StartRoadViewSoloRoundUseCase` | 기존 `execute` 분리 → `prepareInitialContext` (게임·라운드 생성), `startTimer` 제거 |
| application.multi.flow | `PlayerTransitionService` (신규) | 로딩/인트로 ACK 관리, 미도달者 계산, Redis 상태 관리 |
| application.multi.flow | `MultiGameFlowScheduler` (신규) | 일반 타스크 스케줄 API (카운트다운/인트로/로딩 타임아웃) |
| infrastructure.websocket.domain.multi.room | `GameRoomNotificationService` | countdown, move-to-game-page, force-leave 등 메시지 메서드 추가 |
| infrastructure.websocket.domain.multi.round | `GameRoundNotificationService` | intro broadcast, problem payload 전송 책임 추가 |

#### StartRoadViewSoloGameFlowUseCase 의사코드
```
execute(Member host, MultiGameRequest.Start request):
    gameRoom = gameRoomAdaptor.queryByIdFetchHost(request.gameRoomId)
    gameRoom.start(host) // 기존 로직

    context = startRoadViewSoloRoundUseCase.prepareInitialContext(gameRoom, request)
        => game, round, roundDuration, players, problemDto 포함

    playerTransitionService.initialize(roomId, context.playerIds)

    gameRoomRedisService.setRoomState(roomId, COUNTDOWN)
    gameRoomNotificationService.broadcastCountdown(roomId, 3)

    scheduler.schedule(roomId, TASK.COUNTDOWN, Duration.ofSeconds(3),
        () -> triggerPageTransition(roomId, context))

triggerPageTransition(roomId, context):
    gameRoomRedisService.setRoomState(roomId, LOADING)
    gameRoomNotificationService.notifyMoveToGamePage(roomId, context.viewDataForMove)

    scheduler.schedule(roomId, TASK.LOADING_TIMEOUT, Duration.ofSeconds(10),
        () -> handleLoadingTimeout(roomId, context))

PlayerTransitionService.markArrived(roomId, playerId):
    redis.hset(player_loading_status:{roomId}, memberId, true)
    if all arrived:
        scheduler.cancel(roomId, TASK.LOADING_TIMEOUT)
        startIntro(roomId, context)

startIntro(roomId, context):
    gameRoomRedisService.setRoomState(roomId, INTRO)
    gameRoundNotificationService.broadcastIntro(roomId, context.fullRoundPayload)

    scheduler.schedule(roomId, TASK.INTRO, context.introDuration,
        () -> launchRoundTimer(roomId, context))

launchRoundTimer:
    gameRoomRedisService.setRoomState(roomId, IN_ROUND)
    timerCommand = TimerCommand.of(context.game, context.round, matchType=SOLO)
    gameTimerService.startRoundTimer(timerCommand)
    submissionRedisService.resetRound(roundId)
```

### 1.2 Redis 구조
```
room_state:{roomId} -> String (COUNTDOWN, LOADING, INTRO, IN_ROUND, ...)
player_loading_status:{roomId} -> Hash(memberId -> "true"/"false")
room_countdown_task:{roomId} -> taskId (scheduler 관리용)
player_transition_blacklist:{roomId} -> Set(memberId) // 로딩 실패자 기록
```
- TTL 정책: 라운드 종료 시 `PlayerTransitionService.cleanup(roomId)` 호출.

### 1.3 Scheduler 설계
```
MultiGameFlowScheduler
 - schedule(roomId, TaskType, Duration, Runnable)
 - cancel(roomId, TaskType)
 - cancelAll(roomId)
TaskType = COUNTDOWN, LOADING_TIMEOUT, INTRO
```
- 내부적으로 `TaskScheduler` 재사용, taskKey = `flow:{roomId}:{taskType}`.
- `GameTimerService`와 동일 scheduler 사용 시 스레드풀 체크 필요.

### 1.4 WebSocket 채널 & DTO
| 채널 | Direction | Payload 예시 |
| --- | --- | --- |
| `/topic/room/{roomId}/countdown` | 서버 → 클라이언트 | `{ "seconds": 3, "startedAt": 173109... }` |
| `/topic/room/{roomId}/move` | 서버 → 클라이언트 | `{ "target": "ROADVIEW_GAME", "roundId": 12 }` |
| `/app/room/{roomId}/loading/ack` | 클라이언트 → 서버 | `{ "roundId": 12 }` |
| `/topic/game/{roomId}/intro` | 서버 → 클라이언트 | `{ "roundId": 12, "durationMs": 5000, "problem": {...} }` |
| `/topic/game/{roomId}/force-leave` | 서버 → 클라이언트 | `{ "memberId": 5, "reason": "loading-timeout" }` |
| `/topic/game/{roomId}/timer/start` | 서버 → 클라이언트 | 기존 TimerStartMessage |
| `/topic/game/{roomId}/round/result` | 서버 → 클라이언트 | 기존 RoundResult DTO |

### 1.5 로딩 타임아웃 처리 상세
```
handleLoadingTimeout(roomId, context):
    pending = playerTransitionService.findPendingPlayers(roomId)
    for memberId in pending:
        member = memberAdaptor.queryById(memberId)
        leaveGameRoomUseCase.execute(member, roomId)
        gameRoomNotificationService.notifyPlayerLeft(roomId, memberInfo)
        lobbyNotificationService.notifyForcedReturn(memberId)
    playerTransitionService.markDropped(roomId, pending)
    startIntro(roomId, context.withActivePlayers())
```
- 탈주자 제거 후 `context` 업데이트 필요: 플레이어 수 변경 → 라운드/점수 로직 영향? `GamePlayerService`에서 총 인원 감소 반영 검토.

### 1.6 반복 라운드 흐름
- `RoundCompletionEventListener.processNextRound`에서 `NextRoadViewRoundUseCase.execute` 호출 후:
```
nextContext = gameContextFactory.fromNextRound(game, round)
playerTransitionService.initialize(roomId, nextContext.activePlayerIds)
startIntro(roomId, nextContext) // 라운드 사이에는 카운트다운 생략, Intro만 재사용
```
- `NextRoadViewRoundUseCase` 리턴 DTO에 문제/라운드 메타 포함하도록 확장.

### 1.7 예외/모니터링
- 모든 scheduler 콜백에 try-catch + 로깅.
- Prometheus metric (optional): `loading_timeout_counter`, `countdown_start_total`.
- Redis mismatch 시 `CheckAndCompleteRoundEarlyUseCase` warning → alert 설정.

## 2. 프론트엔드 로직 설계

### 2.1 상태 머신 개요
```
WAITING -> COUNTDOWN -> NAVIGATING -> LOADING -> INTRO -> IN_ROUND -> ROUND_RESULT
                                        \--(timeout)-> LOBBY_REDIRECT
ROUND_RESULT -> (transition timer) -> INTRO (다음 라운드) or GAME_FINISHED
GAME_FINISHED -> WAITING_ROOM (버튼/자동)
```
- 각 상태 전환은 WebSocket 메시지를 받는 즉시 반영.

### 2.2 WebSocket 핸들러
```ts
subscribe(`/topic/room/${roomId}/countdown`, onCountdown)
subscribe(`/topic/room/${roomId}/move`, onMoveCommand)
subscribe(`/topic/game/${roomId}/intro`, onIntro)
subscribe(`/topic/game/${roomId}/timer/start`, onTimerStart)
subscribe(`/topic/game/${roomId}/round/result`, onRoundResult)
subscribe(`/topic/game/${roomId}/force-leave`, onForceLeave)
subscribe(`/topic/game/${roomId}/transition`, onTransitionTimer) // 기존
```
- `onMoveCommand`: SPA 라우터로 게임 화면 이동, 상태 `NAVIGATING`.
- 게임 페이지 마운트 후 `postLoadingAck()` 호출.

### 2.3 로딩 ACK 시퀀스
```
onMoveCommand -> navigate -> load resources -> dispatch LOADING_READY
if all assets ready (map, textures 등):
    send(`/app/room/${roomId}/loading/ack`, { roundId })
    상태 = LOADING
로딩 중 에러 시:
    show retry UI, allow user to resend ack (또는 leave)
```
- 10초 내 ack 미전송 시 서버 탈주 처리 → 프론트는 `force-leave` 수신 → 로비 route.

### 2.4 IntroOverlay 처리
```
onIntro(payload):
    setState(INTRO)
    render overlay (프리로드된 문제 정보)
    start local countdown = payload.durationMs
    end -> hide overlay -> 대기 (TimerStartMessage 시 라운드 시작)
```
- Intro 동안 문제 데이터 캐시, 지도/이미지 프리로드.

### 2.5 라운드 타이머 & 제출
- TimerStartMessage 수신 시:
  - 서버 시각과 동기화된 timer hook 사용 (보간).
  - 제출 버튼 클릭 → REST `POST /multi/round/{roundId}/submission` 호출 → 성공 시 UI lock.
  - 제출 성공/실패 처리: 실패 시 재시도 UX, 성공 시 상태 `SUBMITTED`.
  - 타이머 만료 시 자동 제출(빈 값) 로직 협의 필요.

### 2.6 라운드 결과 화면
```
onRoundResult(result):
    setState(ROUND_RESULT)
    render leaderboard, 개인 점수
    start local 10초 countdown (transition 메시지에서도 남은 시간 sync)
```
- transition 메시지 수신 시 남은 시간 업데이트.

### 2.7 마지막 라운드 종료
```
onGameFinished(payload):
    setState(GAME_FINISHED)
    render 최종 랭킹/포인트
    show "방으로 돌아가기" 버튼 -> navigate lobby
    optionally auto redirect after N초
```

### 2.8 에러/재접속 전략
- WebSocket 끊김 감지 → 자동 재연결 시 `GET /multi/games/{roomId}/state` (신규 API)로 현재 라운드/타이머/intro 상태 재동기.
- 로딩 단계 재접속 시 서버가 `player_loading_status` 갱신 후 Intro 재진입하도록 허용.

### 2.9 UX 디테일
- 카운트다운 연출: 큰 숫자 3-2-1, 끝에 진동/사운드.
- 로딩 실패 시: 명확한 에러 모달 + 재시도/나가기 선택.
- 조기 종료 시: 타이머 0으로 떨구며 “모든 플레이어 제출 완료” 표시.
- 결과 화면: transition 타이머 progress bar.

## 3. API & Message 스펙 초안

### 3.1 REST
```
POST /api/multi/rooms/{roomId}/roadview/start
  -> StartRoadViewSoloGameFlowUseCase

GET /api/multi/rooms/{roomId}/state (재접속용)
  response:
    {
      "roomState": "IN_ROUND",
      "roundId": 12,
      "gameId": 3,
      "introRemainingMs": 0,
      "roundRemainingMs": 45000,
      "players": [...]
    }
```

### 3.2 WebSocket DTO 예시
```
CountdownMessage {
  int seconds;
  long serverTimestamp;
}

MoveCommandMessage {
  String target; // "ROADVIEW_GAME"
  Long gameId;
  Long roundId;
}

IntroMessage {
  Long roundId;
  int durationMs;
  ProblemDto problem;
  List<PlayerDto> players;
}

ForceLeaveMessage {
  Long memberId;
  String reason; // "loading-timeout"
}
```

## 4. 테스트 전략
- **단위 테스트**: PlayerTransitionService, MultiGameFlowScheduler timer cancel/trigger 검증.
- **통합 테스트**: MockWebSocket + Redis embedded. 시나리오별 end-to-end Event publishing 검증.
- **부하 테스트**: 다중 플레이어 로딩 ack 동시에 들어올 때 race condition 방지 확인.
- **프론트 E2E**: Cypress/Puppeteer로 카운트다운→로딩→인트로→라운드 흐름 자동화.

## 5. 체크리스트 (개발 순서)
1. StartRoadViewSoloRoundUseCase 리팩터링 (`prepareInitialContext`).
2. PlayerTransitionService + Redis schema 적용.
3. GameRoomNotificationService/GameRoundNotificationService 확장 + DTO 정의.
4. StartRoadViewSoloGameFlowUseCase 구현 및 REST 엔드포인트 연결.
5. Scheduler 통합 및 타이머 콜백 유닛테스트.
6. 프론트 WebSocket 핸들러/상태 머신 구현.
7. 재접속 API, Force leave UX.
8. 팀 모드 고려 사항 backlog 등록.

이 문서의 내용을 기반으로 백엔드/프론트별 구현을 병렬 진행할 수 있다.

