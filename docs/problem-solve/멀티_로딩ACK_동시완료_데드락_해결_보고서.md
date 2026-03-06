# 멀티 로딩 ACK 동시 완료 데드락 해결 보고서

## 1. 원인

- `GameTransitionOrchestrator.handleLoadingAck(...)`는 각 ACK 요청을 독립 처리하고, `allArrived=true`이면 즉시 `onAllPlayersArrived(...)`를 실행한다.
- 기존 구조에는 "한 게임 시작을 단 1회만 허용"하는 단일 승자 게이트가 없다.
- 따라서 동시 ACK 상황에서 동일 `gameId`로 `nextRoadViewRoundUseCase.executeInitial(...)`가 중복 호출될 수 있다.
- 초기 라운드 준비 트랜잭션이 중복 실행되면 `multi_road_view_game` 및 연관 라운드 생성 구간에서 락 경합이 커지고, InnoDB 데드락/락 획득 실패로 이어질 수 있다.

## 2. 원인 분석

### 2.1 호출 경로와 레이스 포인트

- WebSocket 진입: `MultiGameWebSocketController.handleLoadingAck(...)`
- 오케스트레이션: `GameTransitionOrchestrator.handleLoadingAck(...)`
  - Redis ready 기록
  - 로딩 상태 계산/브로드캐스트
  - `allArrived`면 `onAllPlayersArrived(...)` 진입
- 시작 처리: `onAllPlayersArrived(...)` -> `executeInitial(...)`

### 2.2 실제 발생 가능성

결론: **실제로 발생 가능하다.**

재현 흐름:

1. A/B ACK가 거의 동시에 수신
2. 두 요청 모두 ready 상태를 기록
3. 두 요청 모두 `allArrived=true` 관측
4. 두 요청 모두 `onAllPlayersArrived` 진입
5. 두 요청 모두 `executeInitial` 중복 실행 시도
6. 동일 게임 레코드 UPDATE 및 라운드 생성 경합 중 데드락 가능

핵심은 ACK 동시성 그 자체가 아니라, **"게임 시작 트랜잭션 중복 실행"** 이다.

### 2.3 구조적 취약점

- `allArrived` 판단만 있고 idempotency 보장이 없다.
- 시작 전 `cleanupLoadingState`가 먼저 수행되어 실패 시 복구 단서가 약해진다.
- 멀티 인스턴스 환경에서는 JVM 로컬 락으로 문제를 막을 수 없다.

## 3. 해결 방안 분석, 비교

### 방안 A: JVM 로컬 락(`synchronized`)

- 장점: 구현 단순
- 단점: 멀티 인스턴스 무효
- 결론: 부적합

### 방안 B: DB 락 단독(비관적 락)

- 장점: 정합성 강함
- 단점: ACK 폭주 시 DB 락 대기/병목
- 결론: 단독 적용 시 운영 부담

### 방안 C: Redis 락 단독

- 장점: 빠른 중복 차단, 분산 환경 적합
- 단점: TTL/해제 정책만으로는 최종 DB 상태 보장에 약점
- 결론: 단독 적용은 리스크 존재

### 방안 D: Redis 단일 승자 + DB 조건부 전이(권장)

- Redis에서 시작 토큰 선점(SET NX EX)
- 선점 성공 요청만 시작 시도
- DB에서 `PENDING -> IN_PROGRESS` 조건부 전이로 최종 1회 보장
- 중복 요청은 no-op

결론: **현업 관점 최적해**

## 4. 해결 방안 적용

### 4.1 적용 원칙

- deadlock 재시도보다 먼저, 중복 시작 자체를 차단
- "게임 시작 1회" 불변식 강제

### 4.2 적용 내용

1) `GameTransitionOrchestrator.onAllPlayersArrived(...)`

- `currentGameId` 조회 후 Redis game-start lock 획득 시도
- 락 선점 실패 시 즉시 중복 요청 skip
- `cleanupLoadingState`를 `executeInitial` 이후로 이동

2) `RoundPreparationService.prepareInitialRound(...)`

- 기존 `game.startGame()` 직접 호출 제거
- `transitionToInProgressIfPending(gameId)` 성공 시에만 초기 라운드 생성
- 실패 시 이미 시작/비정상 상태로 간주하고 null(no-op) 반환

3) `MultiRoadViewGameRepository`

- 조건부 update 쿼리 추가
  - `status = PENDING`일 때만 `IN_PROGRESS`로 전이
  - `currentRound = 1`, `isFinished = false` 동시 반영

4) Redis 서비스 확장

- 키: `game:room:{roomId}:game:{gameId}:start:lock`
- TTL 30초
- acquire/release 메서드 추가

### 4.3 실제 반영 파일

- `src/main/java/com/kospot/application/multi/flow/GameTransitionOrchestrator.java`
- `src/main/java/com/kospot/application/multi/flow/LoadingPhaseService.java`
- `src/main/java/com/kospot/infrastructure/redis/domain/multi/game/service/MultiGameRedisService.java`
- `src/main/java/com/kospot/domain/multi/game/repository/MultiRoadViewGameRepository.java`
- `src/main/java/com/kospot/domain/multi/game/adaptor/MultiRoadViewGameAdaptor.java`
- `src/main/java/com/kospot/application/multi/round/roadview/RoundPreparationService.java`

### 4.4 기대 효과

- 동시 ACK N건에서도 시작 트랜잭션 1건만 유효 실행
- `multi_road_view_game` 업데이트 경합 급감
- 데드락 트리거(중복 시작) 구조적 제거

## 5. 테스트 내용

### 5.1 단위 테스트 설계

- `onAllPlayersArrived`:
  - 락 선점 성공 1건만 시작 호출
  - 락 선점 실패 요청은 no-op
- `RoundPreparationService.prepareInitialRound`:
  - 조건부 전이 실패 시 null 반환
  - 조건부 전이 성공 시 초기 라운드 생성

### 5.2 통합 테스트 설계(동시성)

- 동일 room/game로 ACK 20~100건 동시 주입
- 검증 항목:
  - `executeInitial` 실행 횟수 = 1
  - `MultiRoadViewGame.status = IN_PROGRESS`
  - 초기 라운드 생성 수 1건
  - deadlock/lock-wait 예외 0건

### 5.3 경계/장애 테스트 설계

- 락 선점 후 프로세스 장애 시 TTL 만료 후 재진입 가능 여부
- timeout 경로와 all-arrived 경로 경쟁 시 상호 모순 상태 미발생 검증
- 멀티 인스턴스 환경 반복 검증

### 5.4 이번 작업에서의 실행 결과

- 추가 테스트 파일: `src/test/java/com/kospot/application/multi/round/roadview/RoundPreparationServiceTest.java`
- 실행 시도: `./gradlew test --tests "com.kospot.application.multi.round.roadview.NextRoadViewRoundUseCaseTest"`
- 결과: `worker.org.gradle.process.internal.worker.GradleWorkerMain` 로딩 실패로 `:compileJava` 단계 중단
- 해석: 테스트 코드 검증 이전에 현재 환경의 Gradle worker 런타임 이슈 선해결 필요

---

### 최종 제안

본 이슈는 **Redis 단일 승자 게이트 + DB 조건부 상태 전이**의 2중 방어가 가장 합리적이다.
이 접근은 deadlock을 사후 재시도로 덮지 않고, 원인인 중복 시작 트랜잭션 자체를 차단한다.
