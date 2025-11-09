# 로드뷰 개인전 Orchestration 확장 명세

## 1. 개요
- 목적: 새로 추가될 카운트다운/로딩/인트로 제어와 같은 orchestration 이벤트를 일관된 패턴으로 정의하고, 백엔드-프론트 간 계약을 명문화한다.
- 범위: `StartRoadViewSoloGameFlowUseCase`(예정)에서 사용할 모든 추가 WebSocket 브로드캐스트와 REST/WebSocket 상호작용.
- 원칙:
  - 채널 네이밍은 기존 prefix(`/topic/room`, `/topic/game`)를 재사용.
  - 모든 메시지는 DTO 클래스를 통해 직렬화하며, enum/타입 필드를 명시해 유연성 확보.
  - 서버 → 클라이언트 broadcast, 클라이언트 → 서버 ack 모두 문서화.

## 2. 아키텍처 개요
```
┌──────────────────┐        ┌────────────────────────┐
│  REST Controller  │        │  WebSocket Controller  │
└───────┬──────────┘        └─────────┬──────────────┘
        │                               │
        ▼                               ▼
┌─────────────────────────┐   ┌────────────────────────────┐
│ StartRoadViewSoloGameFlow│   │ PlayerTransitionController │
│        UseCase (신규)    │   │   (loading/intro ACK)      │
└─────────┬────────────────┘   └─────────┬──────────────────┘
          │                                │
          ▼                                ▼
┌────────────────────┐        ┌──────────────────────────┐
│ PlayerTransition   │        │ GameRoom/Timer/Submission │
│ Service (신규)     │        │ NotificationService 등     │
└────────┬───────────┘        └────────┬─────────────────┘
         │                               │
         ▼                               ▼
┌────────────────────────────┐   ┌────────────────────────────┐
│ Redis (room_state, loading │   │ SimpMessagingTemplate      │
│ status, scheduler metadata)│   │ (broadcast)                │
└────────────────────────────┘   └────────────────────────────┘
```
- UseCase가 orchestration을 담당하며 PlayerTransitionService로 Redis 상태/ACK를 관리.
- NotificationService 계층을 통해 실제 WebSocket 메시지 송신.

## 3. 신규 채널/메시지 명세

### 3.1 카운트다운 이벤트
- 채널: `/topic/room/{roomId}/countdown`
- DTO: `RoomCountdownMessage` (신규)
```0:0:docs/multi_roadview_logic/snippets/RoomCountdownMessage.java
@Getter
@Builder
public class RoomCountdownMessage {
    private Long gameId;
    private int secondsLeft;           // 시작 시 3, 후속 브로드캐스트로 2,1
    private Long serverTimestamp;      // epoch millis
    private CountdownPhase phase;      // READY, RUNNING, CANCELLED
}

public enum CountdownPhase {
    READY, RUNNING, CANCELLED
}
```
- 브로드캐스트 패턴:
  1. 3초 카운트다운 시작 시 `secondsLeft=3`, `phase=READY`.
  2. scheduler가 매초 `secondsLeft` 감소 및 `phase=RUNNING`.
  3. 취소될 경우 `phase=CANCELLED`.

### 3.2 페이지 이동 명령
- 채널: `/topic/room/{roomId}/move`
- DTO: `RoomNavigationMessage` (신규)
```0:0:docs/multi_roadview_logic/snippets/RoomNavigationMessage.java
@Getter
@Builder
public class RoomNavigationMessage {
    private String target;             // e.g. "ROADVIEW_GAME", "LOBBY"
    private Long gameId;
    private Long roundId;
    private Long issuedAt;
    private String reason;             // optional (e.g. "timeout", "host-command")
}
```
- 사용 시나리오:
  - 카운트다운 종료 → 게임 화면으로 이동: `target="ROADVIEW_GAME"`.
  - 로딩 실패자 강제 퇴출 → 로비 이동: `target="LOBBY"`, `reason="loading-timeout"`.

### 3.3 로딩 상태 요청 & 응답
- 서버 → 클라이언트: `/topic/game/{roomId}/loading/request`
  - DTO: `LoadingRequestMessage`
```0:0:docs/multi_roadview_logic/snippets/LoadingRequestMessage.java
@Getter
@Builder
public class LoadingRequestMessage {
    private Long roundId;
    private Long timeoutMs;             // 기본 10000
    private Long serverTimestamp;
}
```
- 클라이언트 → 서버: `/app/room/{roomId}/loading/ack`
  - DTO: `LoadingAckMessage`
```0:0:docs/multi_roadview_logic/snippets/LoadingAckMessage.java
@Getter
@Builder
public class LoadingAckMessage {
    private Long memberId;
    private Long roundId;
    private boolean success;           // false인 경우 에러 코드 포함
    private String errorCode;          // optional
    private Long clientTimestamp;
}
```
- 서버 응답: `/topic/game/{roomId}/loading/status`
  - DTO: `LoadingStatusMessage`
```0:0:docs/multi_roadview_logic/snippets/LoadingStatusMessage.java
@Getter
@Builder
public class LoadingStatusMessage {
    private Long roundId;
    private List<MemberLoadingState> players;
    private boolean allArrived;

    @Getter @Builder
    public static class MemberLoadingState {
        private Long memberId;
        private boolean arrived;
        private Long acknowledgedAt;
    }
}
```

### 3.4 Intro Overlay 이벤트
- 채널: `/topic/game/{roomId}/intro`
- DTO: `IntroOverlayMessage`
```0:0:docs/multi_roadview_logic/snippets/IntroOverlayMessage.java
@Getter
@Builder
public class IntroOverlayMessage {
    private Long gameId;
    private Long roundId;
    private int roundNumber;
    private Long durationMs;           // overlay 유지 시간
    private Long serverTimestamp;
    private ProblemPayload problem;    // 라운드 문제 데이터
}

@Getter
@Builder
public static class ProblemPayload {
    private double targetLat;
    private double targetLng;
    private String streetViewPanoId;
    private String hintText;
    private String regionLabel;
}
```

### 3.5 강제 퇴장 알림
- 채널: `/topic/game/{roomId}/force-leave`
- DTO: `ForceLeaveMessage`
```0:0:docs/multi_roadview_logic/snippets/ForceLeaveMessage.java
@Getter
@Builder
public class ForceLeaveMessage {
    private Long memberId;
    private String reason;             // "loading-timeout", "host-kick", etc.
    private Long serverTimestamp;
}
```

## 4. 이벤트 순서도
```
Host Start Button
      │ HTTP POST /roadview/start
      ▼
[StartRoadViewSoloGameFlowUseCase]
      │ prepare context
      ▼
COUNTDOWN
  ├─ broadcast RoomCountdownMessage (3→2→1)
  └─ after 0:
        RoomNavigationMessage(target=ROADVIEW_GAME)
        LoadingRequestMessage(timeout=10s)

LOADING
  ├─ clients send LoadingAckMessage
  ├─ PlayerTransitionService updates Redis
  ├─ broadcast LoadingStatusMessage (optional polling)
  └─ timeout → ForceLeaveMessage + RoomNavigationMessage(target=LOBBY)

INTRO
  ├─ broadcast IntroOverlayMessage
  └─ intro timer end → TimerStartMessage (기존)

ROUND IN PROGRESS
  ├─ TimerSyncMessage (기존)
  ├─ PlayerSubmissionMessage
  └─ RoundCompletionEvent → RoadViewRoundResponse.PlayerResult

ROUND RESULT / TRANSITION
  ├─ RoundTransitionTimerMessage
  └─ transition end → NextRound → Intro 반복 or GameFinalResult
```

## 5. DTO/채널 구현 지침
1. **DTO 위치**: `com.kospot.presentation.multi.flow.dto.message` (신규 패키지) 추천.  
2. **채널 상수**: `GameRoomChannelConstants`, `MultiGameChannelConstants`에 정적 메서드 추가.  
   - 예: `getRoomCountdownChannel(roomId)`, `getGameLoadingRequestChannel(roomId)`.
3. **NotificationService 확장**:
   - `GameRoomNotificationService` → countdown, move, loading status 메서드 추가.
   - `GameRoundNotificationService` → intro overlay 메서드 추가.
4. **UseCase 내부 순서**:
   - `prepareInitialContext` → `broadcastCountdown` → scheduler 등록 → callback에서 `broadcastMove` + `broadcastLoadingRequest`.
   - PlayerTransitionService는 ACK 수신 시 상태 업데이트 후 모든 인원 도착 시 `broadcastIntro`.

## 6. 예외 처리 패턴
| 상황 | 대응 |
| --- | --- |
| 카운트다운 중 호스트 취소 | `CountdownPhase.CANCELLED` 메시지 송신, roomState=WAITING |
| 로딩 실패자 | ForceLeaveMessage 송신 후 leave usecase 호출, 나머지 인원 intro 진행 |
| Intro 중 일부 이탈 | PlayerTransitionService가 감지 시 즉시 강퇴 or round 참가인원 감소 처리 |
| Redis 중단 | 로컬 fallback 제공: roomState DB 컬럼 활용, ack 허용 범위 제한 |

## 7. 프론트엔드 처리 요약
1. `/topic/room/{id}/countdown` → 숫자 표시, `phase=CANCELLED` 시 UI 되돌림.
2. `/topic/room/{id}/move` → 라우팅. `reason`에 따른 모달 메시지.
3. `/topic/game/{id}/loading/request` → 로딩 UI 활성화, 타임아웃 카운트다운.
4. LoadingAckMessage 송신 후 `/topic/game/{id}/loading/status` 수신 시 팀 상태 표시.
5. IntroOverlayMessage → 문제 프리로드.
6. ForceLeaveMessage → 즉시 로비로 redirect + reason 표시.

## 8. 백로그/확장 아이디어
- 팀전 지원 시 `RoomNavigationMessage`에 `teamAssignments` 추가.
- `LoadingStatusMessage`의 `players` 배열 크기가 클 경우 diff 방식(변경된 유저만) 전송 고려.
- 재접속 시 `/api/multi/rooms/{roomId}/state` 응답에 `currentPhase`, `countdownRemaining`, `introRemaining`, `roundRemaining` 필드 추가.

---

## 9. 구현 체크리스트
1. DTO/채널 상수 생성 및 테스트.
2. NotificationService 확장 메서드 구현.
3. PlayerTransitionService + Redis 키 설계 반영.
4. WebSocket Controller (예: `MultiRoomFlowWsController`) 작성:  
   - `@MessageMapping("/room/{roomId}/loading/ack")` → PlayerTransitionService 호출 → 필요 시 상태 broadcast.
5. StartRoadViewSoloGameFlowUseCase 단계별 구현 + 단위테스트.
6. 프론트 WebSocket 핸들러 업데이트, Router 연동, 상태 머신 보강.
7. 문서와 실제 구현이 일치하는지 통합 테스트 작성.


