# 로드뷰 멀티 개인전 게임 흐름 설계 문서

## 1. 시나리오 개요
- 호스트가 `게임 시작`을 누르면 전 플레이어가 3초 뒤 게임 페이지로 이동해야 하며, 로딩 완료 확인 후 인트로 오버레이 → 라운드 진행 → 결과/전환 → 마지막 라운드 종료까지 이어지는 멀티 라운드 파이프라인을 필요로 한다.
- 현재 구현된 UseCase, 서비스는 라운드 생성·타이머·결과 처리까지 갖추고 있으나, **게임 시작 직후의 카운트다운/로딩 상태 관리, 인트로 오버레이 동기화, 로딩 실패자 처리** 등의 orchestration 계층이 추가로 필요하다.

## 2. 전체 타임라인(개략)
1. `HOST_START_REQUEST` (HTTP) → `StartRoadViewSoloRoundUseCase`.
2. **pre-round 카운트다운 3초**: WebSocket 방송 + 타이머 스케줄.
3. **페이지 전환**: `GameRoomNotificationService` 경유 broadcast, 각 플레이어 ACK 수집.
4. **로딩 검증**: 10초 타임아웃. 미도달자는 `LeaveGameRoomUseCase` 흐름으로 강제 탈주 후 로비 이동.
5. **IntroOverlay**: 문제 패킷 사전 전송, 인트로 타이머 종료 시 라운드 타이머 시작.
6. **라운드 진행**: `GameTimerService.startRoundTimer`, 제출시 `SubmitRoadViewPlayerAnswerUseCase` → `CheckAndCompleteRoundEarlyUseCase`.
7. **라운드 종료**: `RoundCompletionEvent` → `RoundCompletionEventListener` → 결과 브로드캐스트, 전환 타이머.
8. **전환 대기**: `GameTimerService.startRoundTransitionTimer`.
9. **다음 라운드 or 게임 종료**: `NextRoadViewRoundUseCase` 혹은 `FinishMultiRoadViewGameUseCase`.
10. **최종 결과**: broadcast 후 플레이어 로비 복귀 버튼/자동 이동.

## 3. 상태/이벤트 설계
| 단계 | Redis RoomState 제안 | 브로드캐스트 주체 | 관련 타이머 |
| --- | --- | --- | --- |
| 게임 대기 | `WAITING` | 기존 구현 | 없음 |
| 카운트다운 | `COUNTDOWN` | `GameRoomNotificationService.broadcastCountdown(roomId, 3)` (신규) | scheduler 3초 |
| 페이지 이동/로딩 | `LOADING` | `notifyMoveToGamePage` (신규) | 로딩 검증 타이머(10초) |
| 인트로 | `INTRO` | `GameRoundNotificationService.broadcastIntro` (신규) | 인트로 타이머 |
| 라운드 진행 | `IN_ROUND` | 기존 TimerStartMessage | `GameTimerService.startRoundTimer` |
| 라운드 결과 | `ROUND_RESULT` | `broadcastRoundResults` (기존) | `startRoundTransitionTimer` |
| 라운드 전환 | `TRANSITION` | `RoundTransitionTimerMessage` (기존) | transition scheduler |
| 게임 종료 | `FINISHED` | `notifyGameFinishedWithResults` (기존) | 없음 |

## 4. 컴포넌트 책임 정리
- **StartRoadViewSoloRoundUseCase**  
  - 현재: 게임/라운드 생성 및 타이머 즉시 시작.  
  - 조정: 타이머 시작은 Intro 단계 종료 후로 이동. UseCase는 `StartPlayerGameContext`(게임 정보+라운드+문제 DTO)까지 준비하고, 카운트다운/인트로 orchestrator에 전달.
- **GameTimerService**  
  - 라운드 타이머 및 전환 타이머 기능 보유.  
  - 신규 필요: 일반 타이머(카운트다운, 인트로) 지원 API 또는 공용 scheduler 래퍼.  
  - 로딩 타임아웃 관리(10초) 역시 여기 혹은 별도 `AsyncTaskService`에서 schedule.
- **GameRoomRedisService**  
  - 플레이어 목록/팀/세션 관리.  
  - 신규 필요: `setRoomState`, `getRoomState`, 로딩 ACK 저장 (예: `player_loading_status:{roomId}` hash).  
  - 10초 미도달자 추적을 위해 `PlayerTransitionService` (application 계층)에서 Redis 데이터를 가공.
- **GameRoomNotificationService / GameRoundNotificationService**  
  - 기존 브로드캐스트 기능 확장: 카운트다운, 페이지 이동, Intro, 강제 퇴장 알림 등 채널 정의.
- **SubmitRoadViewPlayerAnswerUseCase**  
  - 제출 시 `CheckAndCompleteRoundEarlyUseCase` 호출하도록 확인/보완.
- **RoundCompletionEventListener**  
  - 기존 흐름이 완전하며, Intro/카운트다운 단계와 결합 시 라운드 재진입 가능.
- **새 orchestration UseCase 제안**  
  - `StartRoadViewSoloGameFlowUseCase` (가칭): 전체 pre-round 파이프라인 orchestrate.  
  - 내부에서:  
    1. `StartRoadViewSoloRoundUseCase` 호출(라운드 생성만).  
    2. Redis 상태 `COUNTDOWN` 설정 및 카운트다운 브로드캐스트.  
    3. 3초 후 `triggerGamePageTransition`.  
    4. 10초 로딩 타이머/ACK 검증 → `startIntroOverlay`.
    5. Intro 타이머 종료 시 `GameTimerService.startRoundTimer`.

## 5. 단계별 상세 설계
### 5.1 게임 시작 (호스트 액션)
- 엔드포인트: `POST /multi/games/{roomId}/roadview/start` (가정).  
- 플로우:
  1. `StartRoadViewSoloGameFlowUseCase.execute(host, request)`
  2. `GameRoomAdaptor.queryByIdFetchHost` → 검증.
  3. `StartRoadViewSoloRoundUseCase.prepareInitialRound(...)`  
     - 기존 `startRoadViewGame`을 분리: `createGame`, `createPlayers`, `createFirstRound`, 문제 DTO 구성.
     - 타이머 시작 대신 `RoundStartContext` 반환.
  4. Redis 상태 `COUNTDOWN` 기록, 카운트다운 브로드캐스트.
  5. `GameTimerService.scheduleGeneralTimer(roomId, duration=3s, callback=triggerGamePageTransition)`

### 5.2 페이지 이동 및 로딩 검증
- `triggerGamePageTransition` 수행:  
  - RoomState `LOADING`.  
  - `GameRoomNotificationService.notifyMoveToGamePage(roomId, roundContext)`  
  - `PlayerTransitionService.initializeLoading(roomId, playerIds)` → Redis hash `player_loading_status:{roomId}` = false.  
  - 로딩 타임아웃 타이머(10초) 시작.
- 플레이어가 접속 완료 시 프론트 → WebSocket `/app/multi/room/{id}/loading-complete`.  
  - 핸들러 → `PlayerTransitionService.markArrived(roomId, memberId)`.  
  - 모든 인원이 true가 되면 로딩 타이머 취소 후 Intro 단계 진행.
- 타임아웃 발생 시 `handleLoadingTimeout`:  
  - 미도달 목록 수집.  
  - `GameRoomService.leaveGameRoom` + `GameRoomRedisService.removePlayerFromRoom`.  
  - `GameRoomNotificationService.notifyPlayerLeft`.  
  - 필요 시 호스트 재지정은 기존 `GameRoomEventHandler.changeHostIfNeeded`에 위임.  
  - 남은 인원 대상으로 Intro 진행.

### 5.3 IntroOverlay
- `startIntroOverlay(roomId, context)`  
  - RoomState `INTRO`.  
  - `GameRoundNotificationService.broadcastIntro(roomId, context)`  
    - 포함 정보: 라운드 ID, 문제 데이터, 라운드 제한 시간, 플레이어 리스트 등.  
  - IntroDuration(프론트 협의, 예: 5초) 동안 타이머 스케줄.  
  - 종료 콜백에서 `launchRoundTimer`.

### 5.4 라운드 진행
- `launchRoundTimer(roomId, context)`  
  - RoomState `IN_ROUND`.  
  - `GameTimerService.startRoundTimer(TimerCommand)` 호출 (기존 로직).  
  - Sync/Completion 이벤트는 현행대로 동작.
- 제출 플로우:  
  - `SubmitRoadViewPlayerAnswerUseCase` → `SubmissionRedisService.increment` → `CheckAndCompleteRoundEarlyUseCase`.  
  - 모든 제출 완료 시 `EarlyRoundCompletionEvent` 발생 → `RoundCompletionEventListener`.

### 5.5 라운드 종료 이후
- `RoundCompletionEventListener`에서 라운드 결과 계산(`EndRoadViewSoloRoundUseCase`) 및 브로드캐스트.  
- `GameTimerService.startRoundTransitionTimer`로 10초 전환 대기.  
- 콜백에서 `processNextRound`:  
  - 게임이 마지막 라운드인지 판단.  
  - 아니면 `NextRoadViewRoundUseCase` 호출 → 새 round context 생성 → **다시 Intro Flow** 시작 (카운트다운 없이 바로 Intro? → 요구사항 검토 필요).  
  - 요구사항 상 “같은 방식”이라면 Intro 전 카운트다운 재사용 여부 결정. 권장: 전환 후 Intro overlay만 수행, 추가 카운트다운은 생략.

### 5.6 게임 종료
- `FinishMultiRoadViewGameUseCase` → 포인트 부여, 최종 결과 브로드캐스트.  
- RoomState `FINISHED`.  
- 프론트에서 “나가기” → `LeaveGameRoomUseCase`.  
- 혹은 일정 시간 후 자동 로비 이동을 위해 추가 타이머 설정 가능.

## 6. 필요한 신규/개선 항목 정리
- **UseCase 리팩터링**
  - `StartRoadViewSoloRoundUseCase` → `prepareGameContext` 메서드 분리, 라운드 타이머 직접 호출 제거.
  - 신규 `StartRoadViewSoloGameFlowUseCase` (orchestrator) 추가.
- **Redis 스키마**
  - `room_state:{roomId}` (string).  
  - `player_loading_status:{roomId}` (hash: memberId → bool).  
  - `player_intro_ack:{roomId}` (필요 시).  
  - TTL/정리 전략 정의.
- **타이머 유틸**
  - GameTimerService에 일반 목적 타이머 API 추가 또는 `GameFlowScheduler` 클래스 도입.  
  - 카운트다운/인트로/로딩 타임아웃을 동일한 scheduler로 관리.
- **WebSocket 채널 확장**
  - `/topic/room/{roomId}/countdown`  
  - `/topic/room/{roomId}/transition` (existing)  
  - `/topic/room/{roomId}/loading` (ACK 요청/응답)  
  - `/topic/game/{roomId}/intro` (문제 패킷)  
  - `/topic/game/{roomId}/force-leave` (탈주 통지)
- **프론트 요구 인터페이스**
  - 카운트다운 메시지 스펙, 로딩 ack request/response 포맷 정의.  
  - IntroOverlay duration & 문제 데이터 구조 협의.  
  - 결과 화면 10초 표시 후 자동/수동 전환 UX 확정.

## 7. 예외 및 엣지 케이스
- 호스트가 카운트다운 중 이탈: 자동 호스트 변경 후 프로세스 유지 or 카운트다운 취소 → 새 호스트가 재시작.  
- 플레이어가 인트로 중 이탈: 라운드 시작 직전까지 취소 가능 여부 판단. 기본은 `LeaveGameRoomUseCase` 처리.  
- Redis와 DB 불일치: `CheckAndCompleteRoundEarlyUseCase` 내 로그 이미 존재. 모니터링 강화 필요.  
- 타이머 스케줄러 장애 대비: 중복 실행 방지, 재시작 시 상태 복구 전략 (room_state 기반 재동기).  
- 팀전 전환 대비: 제출 기대 수 계산 로직/Intro payload 차등화.

## 8. 후속 작업 로드맵
1. `StartRoadViewSoloRoundUseCase` 분리 및 기존 호출부 영향도 분석.  
2. 신규 orchestration UseCase + Transition Service 구현.  
3. Redis/Notification/Timer 계층 보강.  
4. WebSocket 엔드포인트 및 DTO 정의.  
5. 통합 테스트:  
   - 전 플레이어 정상 진행 플로우.  
   - 로딩 타임아웃/탈주.  
   - 조기 종료와 타임아웃 종료 각각.  
   - 마지막 라운드 종료 후 포인트 적립 검증.


