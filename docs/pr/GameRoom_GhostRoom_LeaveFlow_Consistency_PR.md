# PR: 게임방 유령방/퇴장 흐름 정합성 안정화

## 배경

기존 게임방 흐름에서 다음 문제가 간헐적으로 재현되었다.

- leave 이후 실제 인원 0명인데 room 데이터가 남는 유령방 발생
- `member.gameRoomId` stale 상태로 인해 join/create가 차단
- disconnect/unsubscribe/navigate 전환 경로마다 leave 정책이 달라 정리 누락 발생
- Redis/DB 불일치를 즉시 실패로 처리하면서 사용자 액션까지 중단

이번 PR은 신규 기능보다 **정합성/복원력 강화**에 초점을 맞춘 안정화 작업이다.

---

## 변경 범위 요약

### 1) Leave 멱등화 + 결과 모델 도입

핵심 파일:

- `src/main/java/com/kospot/multi/room/application/usecase/LeaveGameRoomUseCase.java`
- `src/main/java/com/kospot/multi/room/application/vo/LeaveRoomResult.java`
- `src/main/java/com/kospot/common/lock/vo/HostAssignmentResult.java`
- `src/main/java/com/kospot/common/lock/strategy/LuaScriptStrategy.java`

핵심 내용:

- leave 반환값을 `void` -> `LeaveRoomResult`로 변경
- `ALREADY_LEFT`, `ROOM_NOT_FOUND_CLEANED`를 성공성 결과로 처리
- Redis player 미존재(`Player not found`)를 hard failure가 아닌 멱등 상태로 해석
- retryable/fatal 실패를 분리(`FailureType`)해 장애 신호 은닉 방지

### 2) join/create 자동 보정 강화 (`REQUIRES_NEW`)

핵심 파일:

- `src/main/java/com/kospot/multi/room/application/service/AutoLeaveRecoveryService.java`
- `src/main/java/com/kospot/multi/room/application/vo/ReconcileResult.java`
- `src/main/java/com/kospot/multi/room/application/usecase/JoinGameRoomUseCase.java`
- `src/main/java/com/kospot/multi/room/application/usecase/CreateGameRoomUseCase.java`

핵심 내용:

- `AutoLeaveRecoveryService`에서 선행 leave를 독립 트랜잭션으로 수행
- recoverable 실패는 흡수 후 `member.gameRoomId` force detach로 복구
- fatal 실패만 join/create를 차단

### 3) 이벤트 정합성 강화 (AFTER_COMMIT)

핵심 파일:

- `src/main/java/com/kospot/multi/room/application/handler/GameRoomEventHandler.java`

핵심 내용:

- room join/leave 이벤트 리스너를 `@TransactionalEventListener(AFTER_COMMIT)`로 전환
- 트랜잭션 롤백 시 알림 선반영되는 케이스 완화

### 4) WebSocket 이탈 경로 통합

핵심 파일:

- `src/main/java/com/kospot/multi/room/application/service/RoomExitOrchestrator.java`
- `src/main/java/com/kospot/common/websocket/handler/WebSocketEventHandler.java`
- `src/main/java/com/kospot/common/websocket/connection/handler/WebSocketConnectionStateEventHandler.java`
- `src/main/java/com/kospot/common/websocket/session/service/WebSocketSessionService.java`

핵심 내용:

- `RoomExitOrchestrator`로 WS leave 호출 정책 일원화
- unsubscribe 시 "마지막 room subscription 해제"인 경우에만 leave 트리거
- grace-expired 이벤트도 동일 오케스트레이터 경로를 사용

### 5) 정합성 cleanup + 부하 안정화

핵심 파일:

- `src/main/java/com/kospot/multi/room/infrastructure/scheduler/GameRoomConsistencyCleanupScheduler.java`
- `src/main/java/com/kospot/multi/room/infrastructure/redis/service/GameRoomRedisService.java`
- `src/main/java/com/kospot/member/infrastructure/persistence/MemberRepository.java`
- `src/main/java/com/kospot/member/application/adaptor/MemberAdaptor.java`
- `src/main/java/com/kospot/member/domain/entity/Member.java`
- `src/main/resources/application.yml`

핵심 내용:

- feature flag 기반 consistency cleanup scheduler 추가
- Redis room key 탐색을 `KEYS`에서 `SCAN`으로 전환
- `member(game_room_id)` 인덱스 추가
- join 경로의 디스크 debug append 제거

---

## 부작용/리스크 대응

1. leave 완화로 실제 장애가 묻힐 수 있는 리스크
- `RETRYABLE_FAILURE` / `FATAL_FAILURE`를 분리해 처리

2. cleanup 배치 부하 리스크
- `enabled`, `fixed-delay-ms`, `batch-size` 설정으로 점진 적용 가능

3. WS 중복 leave 트리거 리스크
- unsubscribe 시 잔여 room subscription 확인 후 처리

---

## 테스트 및 검증

반영 테스트:

- 신규: `src/test/java/com/kospot/application/multi/room/http/usecase/AutoLeaveRecoveryServiceTest.java`
- 기존 leave 테스트 시그니처 정리:
  - `src/test/java/com/kospot/application/multi/room/http/usecase/LeaveGameRoomUseCaseTest.java`
  - `src/test/java/com/kospot/application/multi/room/http/usecase/LeaveGameRoomUseCaseConcurrencyTest.java`
  - `src/test/java/com/kospot/multi/room/usecase/GameRoomUseCaseTest.java`

환경 제약:

- 로컬 Gradle Worker 런타임 이슈로 컴파일 검증 실패
- 에러: `Could not find or load main class worker.org.gradle.process.internal.worker.GradleWorkerMain`

---

## 운영 적용 가이드

1. 1차 배포: scheduler 비활성(`GAME_ROOM_CONSISTENCY_CLEANUP_ENABLED=false`)로 반영
2. 메트릭/로그 관찰 후 scheduler 단계적 활성화
3. `batch-size`, `fixed-delay-ms`를 Redis/DB 부하 기준으로 튜닝

---

## 관련 커밋

- `bb42039b` refactor(room): make leave idempotent and resilient for transitions
- `20c71ef4` feat(websocket): unify room leave handling across unsubscribe and grace expiry
- `342f1e7a` feat(room-consistency): add scan-based cleanup for ghost rooms and dangling members
- `81ba3106` docs(room): add ghost-room consistency refactoring plan
