# Daily MVP 동시성 안정화 및 Reconcile 전환 PR

## 1. PR 목적

이번 PR은 `docs/pr/Daily_MVP_Concurrency_Task_Breakdown.md` 계획의 P0~P2 핵심 구간을 구현하여,
일일 MVP 집계/보상 경로의 동시성 안정성을 높이고 hourly 배치를 reconcile 중심으로 전환하는 것이 목적입니다.

핵심 목표는 다음과 같습니다.

- MVP 후보 비교 규칙을 도메인 정책으로 분리(A-1/A-2/A-3)
- 보상 중복 지급 방지를 Redis 기반 idempotency로 강화(D-1)
- 게임 종료 이벤트 기반 증분 집계 파이프라인 도입(B-1/B-2/B-3/B-4 핵심)
- hourly 배치를 full-scan 집계에서 reconcile/복구 배치로 전환(C-1/C-2)

---

## 2. 변경 요약

### 2-1) 도메인 정책 분리 (A-1, A-2, A-3)

#### 추가
- `src/main/java/com/kospot/mvp/domain/vo/MvpCandidateSnapshot.java`
  - 후보 판단/반영에 필요한 필드를 캡슐화한 스냅샷 VO
  - 팩토리 메서드 제공
    - `from(RoadViewGame, GameRank)`
    - `from(DailyMvp, LocalDateTime)`
  - `validate()`로 필수 필드 null 방어

- `src/main/java/com/kospot/mvp/domain/policy/MvpCandidateComparator.java`
  - 비교 규칙 단일화
  - 우선순위: `score DESC` -> `endedAt ASC` -> `roadViewGameId ASC`

#### 변경
- `src/main/java/com/kospot/mvp/application/service/DailyMvpAggregationService.java`
  - 기존 단순 갱신에서 `shouldReplace(...)` 판단 기반 갱신으로 리팩토링
  - 동일 후보/열등 후보는 스냅샷 업데이트를 건너뜀

#### 테스트
- `src/test/java/com/kospot/mvp/domain/policy/MvpCandidateComparatorTest.java`
  - 점수 우선, 종료시간 tie-break, id tie-break 검증
- `src/test/java/com/kospot/mvp/service/DailyMvpAggregationServiceTest.java`
  - Comparator 주입 반영

---

### 2-2) 보상 idempotency 강화 (D-1)

#### 변경
- `src/main/java/com/kospot/common/redis/common/constants/RedisKeyConstants.java`
  - 보상 lock/done 키 패턴 추가

- `src/main/java/com/kospot/mvp/infrastructure/redis/dao/DailyMvpCacheRedisRepository.java`
  - `setIfAbsent(key, value, ttl)` 추가
  - `exists(key)` 추가

- `src/main/java/com/kospot/mvp/infrastructure/redis/service/DailyMvpCacheService.java`
  - 보상 잠금/중복 처리용 API 추가
    - `tryAcquireRewardLock(...)`
    - `releaseRewardLock(...)`
    - `isRewardProcessed(...)`
    - `markRewardProcessed(...)`

- `src/main/java/com/kospot/mvp/application/service/DailyMvpRewardService.java`
  - 처리 흐름 강화:
    1) 날짜 row lock 조회
    2) Redis reward lock 획득 시도
    3) 이미 처리된 건인지 체크
    4) 포인트 지급/이력 저장/이벤트 발행
    5) `afterCommit`에서 done 마킹
    6) `afterCompletion`에서 lock 해제
  - 예외 발생 시 즉시 reward lock 해제

#### 테스트
- `src/test/java/com/kospot/mvp/service/DailyMvpRewardServiceTest.java`
  - Redis lock + 처리 여부 분기 mocking 반영

---

### 2-3) 증분 집계 파이프라인 도입 (B-1, B-2, B-3, B-4 핵심)

#### 추가
- `src/main/java/com/kospot/mvp/application/listener/DailyMvpIncrementalAggregationEventListener.java`
  - `RoadViewRankEvent`를 `AFTER_COMMIT`에서 수신
  - `mvp.incremental.enabled` 플래그 기반 동작
  - `RANK + COMPLETED` 조건에서만 증분 집계 수행

- `src/main/java/com/kospot/mvp/application/service/DailyMvpIncrementalAggregationService.java`
  - 흐름:
    1) 후보 스냅샷 생성
    2) Redis CAS(compare-and-set)로 후보 교체 시도
    3) 교체 성공 시 DB 스냅샷 반영
    4) 반영 성공 시 조회 캐시 evict

- `src/main/java/com/kospot/mvp/infrastructure/redis/dao/DailyMvpCandidateRedisRepository.java`
  - Lua 스크립트 기반 원자 compare-and-set 구현
  - 키: `mvp:daily:candidate:{date}:v1`
  - 결과: 교체 성공(1)/유지(0)
  - 후보 스냅샷 필드(`poiName` 포함) 저장/복구

- `src/main/java/com/kospot/mvp/infrastructure/redis/service/DailyMvpCandidateCacheService.java`
  - 후보 캐시 접근 서비스 레이어

#### 변경
- `src/main/java/com/kospot/mvp/domain/entity/DailyMvp.java`
  - snapshot 기반 생성/갱신 API 추가
    - `create(LocalDate, MvpCandidateSnapshot, int)`
    - `updateSnapshot(MvpCandidateSnapshot)`

#### 테스트
- `src/test/java/com/kospot/mvp/service/DailyMvpIncrementalAggregationServiceTest.java`
  - CAS 실패 시 DB 반영 없음
  - CAS 성공 + row 없음 시 신규 저장 및 캐시 무효화 검증

---

### 2-4) reconcile 배치 전환 및 불일치 복구 (C-1, C-2)

#### 추가
- `src/main/java/com/kospot/mvp/application/service/DailyMvpReconcileService.java`
  - 최근 2일(어제/오늘) 대상 정합성 점검
  - 케이스별 처리:
    1) Redis 후보 없음: fallback 플래그 on일 때만 full-scan 집계 실행
    2) DB row 없음: Redis 후보 스냅샷으로 `DailyMvp` 생성
    3) DB/Redis 불일치: 비교 정책 기반으로 더 우수한 후보로 수렴
       - Redis 후보 우위: DB 갱신 + 캐시 evict
       - DB 후보 우위: Redis 후보 캐시 복구(compare-and-set)

#### 변경
- `src/main/java/com/kospot/mvp/application/scheduler/DailyMvpScheduler.java`
  - `aggregateHourly()` 기본 동작을 reconcile 모드로 변경
  - `mvp.reconcile-only.enabled=true`(기본)일 때 `reconcileRecent()` 실행
  - 필요 시 기존 full-scan 집계(`aggregateToday()`)로 플래그 기반 fallback 가능

#### 테스트
- `src/test/java/com/kospot/mvp/service/DailyMvpReconcileServiceTest.java`
  - 후보 캐시 미스 + fallback off -> no-op
  - DB row 누락 -> 생성 + 캐시 무효화
  - DB가 더 우수 -> Redis 후보 캐시 복구

---

## 3. 동시성/정합성 개선 포인트

- 비교 규칙 중앙화로 후보 판단 로직의 분산/불일치 위험 축소
- 보상 경로에 Redis lock + done marker를 추가해 배치 재진입/중복 실행 내성 강화
- 증분 집계에서 Lua CAS를 사용해 분산 환경 후보 교체 경쟁 구간을 원자화
- 후보 교체 성공 시에만 DB/캐시 갱신하여 불필요한 쓰기 감소 및 stale window 축소
- reconcile 배치가 Redis/DB 불일치를 idempotent하게 수렴시켜 장애 후 정합성 복원력 강화

---

## 4. 커밋 내역

1) `9f1a7fda`  
`refactor mvp candidate comparison into domain policy`

2) `d90a4fd1`  
`add redis-backed idempotency guard for mvp rewards`

3) `08fcab1d`  
`introduce incremental daily mvp aggregation pipeline`

4) `db8fb381`  
`switch hourly mvp job to reconcile with cache/db repair`

---

## 5. 검증 결과

로컬에서 Gradle 테스트/컴파일을 실행했으나, 환경 이슈로 Worker 프로세스가 기동되지 않아 자동 검증이 완료되지 않았습니다.

- 오류 요약:
  - `Could not find or load main class worker.org.gradle.process.internal.worker.GradleWorkerMain`
  - `Execution failed for task ':compileJava'`

즉, 코드 변경/커밋은 완료했지만 CI 또는 Gradle 환경 정상화 후 재검증이 필요합니다.

---

## 6. 리스크 및 후속 작업

### 현재 PR에서 의도적으로 남긴 범위
- D-2(DB 유니크 제약/마이그레이션)는 별도 PR 권장
- E-1/E-2/E-3(메트릭/구조화 로그/런북)은 후속 PR 권장

### 다음 PR 권장 항목
1. 포인트 이력 자연키 기반 유니크 제약 추가(D-2)
2. 메트릭/구조화 로그(E-1/E-2) 및 런북(E-3) 반영
3. 동시성 통합 테스트(100~500 동시 이벤트) 및 장애 복구 시나리오 테스트 추가

---

## 7. 체크리스트

- [x] MVP 후보 비교 정책 객체 도입 및 테스트 추가
- [x] 기존 집계 서비스 정책 연동
- [x] 보상 idempotency lock/done 키 도입
- [x] 보상 서비스 트랜잭션 후처리(커밋 후 done 마킹) 반영
- [x] 이벤트 기반 증분 집계 리스너/서비스 도입
- [x] Redis Lua CAS 기반 후보 교체 구현
- [x] 후보 교체 시 조회 캐시 무효화 연결
- [x] hourly 배치 reconcile 전환
- [x] Redis/DB 불일치 탐지 및 복구 로직 추가
- [ ] Gradle 환경 이슈 해소 후 전체 테스트/컴파일 재검증
