# Daily MVP 동시성 안정화 실행 계획

## 1) 목적과 완료 기준

### 목적
- 일일 MVP 선정 경로를 **동시성 안전**하게 재설계한다.
- 시간 배치 중심 구조를 **증분 집계 + 검증 배치** 구조로 전환한다.
- 보상 지급을 **idempotent(중복 지급 불가)** 하게 만든다.
- 장애/재시작/멀티 인스턴스 환경에서 정합성을 유지한다.

### 완료 기준 (Definition of Done)
- 동일 날짜에 동시 이벤트가 유입되어도 최종 MVP가 정렬 규칙(`score DESC`, `endedAt ASC`, `id ASC`)과 100% 일치한다.
- 낮은 점수 이벤트가 높은 점수를 덮어쓰는 현상이 발생하지 않는다.
- 보상 배치 재실행/중복 실행 시 중복 포인트 지급 0건을 보장한다.
- Redis 장애 이후 복구 배치 실행으로 DB 스냅샷 정합성이 회복된다.
- 조회 API(`GET /mvps`) 응답 스펙은 변경하지 않는다.

---

## 2) 현재 구조와 리스크 요약

대상 코드:
- `src/main/java/com/kospot/mvp/application/service/DailyMvpAggregationService.java`
- `src/main/java/com/kospot/mvp/application/scheduler/DailyMvpScheduler.java`
- `src/main/java/com/kospot/game/infrastructure/persistence/RoadViewGameRepository.java`
- `src/main/java/com/kospot/mvp/application/service/DailyMvpRewardService.java`
- `src/main/java/com/kospot/mvp/application/usecase/GetDailyMvpUseCase.java`

핵심 리스크:
- **경쟁 상태**: 집계가 `query -> update/save` 패턴이라 동시성 시 last write wins 가능.
- **비효율**: 매시간 배치가 동일 날짜 범위를 반복 스캔/정렬한다.
- **분산 한계**: 스케줄 락은 중복 실행 완화 수준이며, 후보 비교-교체의 원자성을 보장하지 못한다.
- **보상 안전성 강화 필요**: 현재 락/플래그가 있어도 중복 적립 방지를 DB 제약으로 강하게 고정할 필요가 있다.

---

## 3) 설계 원칙 (클린 코드 + 동시성)

- **단일 책임**: 후보 계산, 후보 저장, 보상 지급, 조회 캐시는 명확히 분리한다.
- **정책 분리**: MVP 비교 규칙을 도메인 정책 객체로 추출하여 테스트 가능하게 만든다.
- **원자성 우선**: 동시성 경쟁 구간은 Redis Lua 또는 DB 조건부 갱신으로 원자 보장.
- **Idempotency 기본값**: 재시도 가능한 경로(배치/이벤트)는 중복 실행 안전성을 먼저 설계.
- **관측 가능성**: 실패는 숨기지 않고 메트릭/로그로 운영자가 즉시 판단 가능해야 한다.

---

## 4) 목표 아키텍처

핵심: **실시간 증분 후보 갱신 + 저주기 검증 배치 + 자정 보상 확정**

### 흐름
1. RANK 게임이 COMPLETED 되면 후보 평가 이벤트 발생
2. Redis `mvp:daily:candidate:{date}`에서 Lua로 원자 비교-교체 수행
3. 후보가 실제 교체된 경우에만 DB `daily_mvp`를 조건부 업데이트
4. 조회는 기존 캐시 전략 유지, 후보 교체 시 해당 일자 캐시만 evict
5. 배치는 메인 계산이 아니라 Redis/DB 불일치 복구에 집중
6. 보상 배치는 `FOR UPDATE` + idempotency key + unique 제약으로 최종 안전성 확보

---

## 5) 상세 설계

## 5-1. 도메인 정책 객체 도입
- `MvpCandidateComparator` (신규): 우선순위 비교 규칙 캡슐화
- `MvpCandidateSnapshot` (신규 VO): `memberId`, `roadViewGameId`, `score`, `endedAt`, `rankTier`, `rankLevel`, `ratingScore`

효과:
- 비교 규칙 분산 제거
- 테스트에서 경계 케이스(동점, 시간 역전) 재현 용이

## 5-2. Redis 원자 비교-교체

키:
- `mvp:daily:candidate:{yyyy-MM-dd}` (Hash 또는 JSON)

필드(예시):
- `memberId`, `roadViewGameId`, `score`, `endedAtEpochMilli`, `rankTier`, `rankLevel`, `ratingScore`, `updatedAt`

갱신 방식:
- Lua 스크립트에서 기존 후보와 신규 후보를 비교
- 신규가 우수할 때만 값 교체 후 `1` 반환, 아니면 `0`

원칙:
- Java에서 get/compare/set 3단계를 분리하지 않는다.
- 반드시 Redis 서버 측 원자 연산으로 처리한다.

## 5-3. DB 조건부 업데이트

`daily_mvp` 반영 규칙:
- Redis 교체 성공(반환 `1`) 시에만 DB 반영 시도
- DB 업데이트는 조건부 쿼리 사용
  - 신규 후보가 더 우수한 경우에만 UPDATE
  - 기존 row 없음 시 INSERT (upsert 패턴)

권장 구현:
- repository custom 쿼리로 `update ... where mvp_date = :date and (기존보다 우수 조건)`
- `updatedRows == 0`이고 row 없음이면 insert 시도
- insert 충돌(unique) 시 재조회 후 조건부 update 재시도(짧은 retry)

## 5-4. 스케줄러 역할 변경

기존:
- 매시간 전체 후보 재계산

변경:
- `aggregation` 스케줄은 "검증/복구" 작업 수행
  - Redis 후보와 DB 스냅샷 불일치 탐지
  - DB 누락/역전 시 복구 반영
- 주 계산은 이벤트 기반 증분 경로가 담당

## 5-5. 보상 idempotency 강화

적용 항목:
- 포인트 이력에 자연키 성격 제약 추가 검토
  - 예: `(member_id, point_history_type, ref_date)` 유니크
- 보상 처리 단위에 idempotency key 적용
  - 예: `mvp-reward:{mvpDate}:{memberId}`
- 현재 `rewardGranted` 플래그 + `FOR UPDATE`는 유지하고, DB 제약으로 2중 방어

## 5-6. 캐시 정합성

- 후보 교체 성공 시 `DailyMvpCacheService.evict(date)` 호출 유지
- None 캐시(`cacheNone`)는 이벤트 기반 갱신 도입 후 오검출 없도록 정책 조정
  - 후보가 생기면 None 키를 즉시 삭제

---

## 6) 트랜잭션/락 경계

- **이벤트 처리 트랜잭션**: Redis 원자 갱신(외부 자원) + DB 조건부 반영(로컬 트랜잭션)으로 분리
- **보상 트랜잭션**: 한 날짜 단위 row lock(`findByMvpDateForUpdate`) 유지
- **분산 락 최소화**: 후보 갱신은 락 기반이 아닌 원자 비교 갱신 기반으로 처리
- **재시도 정책**: unique 충돌/낙관적 충돌 시 짧은 backoff로 제한 재시도(예: 2~3회)

---

## 7) 단계별 실행 계획 (P0/P1/P2/P3)

## P0 - 안전장치 선반영
목표: 중복 지급/경쟁 리스크를 즉시 축소

작업:
- [ ] MVP 비교 규칙 도메인 정책 객체 도입(아직 호출 경로는 기존 유지)
- [ ] 보상 idempotency key 및 중복 지급 방지 제약 설계 확정
- [ ] 핵심 메트릭/로그 추가(후보 교체 시도, 충돌, 보상 중복 차단)

완료 조건:
- 비교 규칙 테스트 코드 확보
- 보상 중복 재현 시도 시 DB 레벨에서 차단 확인

## P1 - 증분 후보 갱신 경로 도입 (Feature Flag)
목표: 이벤트 기반 후보 갱신의 원자성 확보

작업:
- [ ] 게임 종료 이벤트 핸들러 추가(RANK + COMPLETED)
- [ ] Redis Lua compare-and-set 구현
- [ ] 후보 교체 성공 시 DB 조건부 반영 + 캐시 evict
- [ ] 플래그: `mvp.incremental.enabled`

완료 조건:
- 동시 이벤트 테스트에서 역전 갱신 0건
- 기존 조회 API 영향 없음

## P2 - 배치 역할 전환
목표: hourly full scan 제거, 검증/복구 중심으로 전환

작업:
- [ ] `aggregateHourly`를 reconcile 작업으로 변경
- [ ] 불일치 탐지/복구 로직 구현
- [ ] 장애 대비 fallback 문서화(증분 경로 장애 시 임시 full scan)

완료 조건:
- 운영에서 full scan 호출 빈도 급감
- 복구 배치로 불일치 자동 수렴

## P3 - 운영 고도화
목표: 멀티 인스턴스 운영 안정성 완성

작업:
- [ ] 알람 임계치 확정 및 대시보드 반영
- [ ] 리허설: Redis 재시작/지연/부분 장애 시나리오
- [ ] 런북(runbook) 작성

완료 조건:
- 장애 대응 시나리오별 복구 시간(SLO) 충족

---

## 8) 테스트 전략

### 단위 테스트
- 후보 비교 규칙: 점수 우선, 동점 시 endedAt, 동점 시 id
- Lua 결과 해석: 교체 성공/실패 분기
- 조건부 update 쿼리: 우수 후보만 반영

### 통합 테스트
- 동일 날짜 동시 100~500건 이벤트 유입
- out-of-order 이벤트 도착(늦게 온 저득점 이벤트)
- 멀티 인스턴스 2~3대 동시 처리
- 보상 배치 재실행 시 중복 지급 차단

### 장애 테스트
- Redis 일시 장애에서 fail-safe 동작 확인
- DB unique 충돌 재시도 동작 확인
- 복구 배치가 정합성 회복하는지 검증

---

## 9) 관측성

필수 메트릭:
- `mvp_candidate_update_total{result=applied|rejected|error}`
- `mvp_candidate_conflict_retry_total`
- `mvp_reconcile_total{result=fixed|noop|error}`
- `mvp_reward_total{result=success|duplicate_blocked|error}`

필수 로그 필드:
- `mvpDate`, `roadViewGameId`, `memberId`, `score`, `endedAt`, `compareResult`, `instanceId`

알람 제안:
- `error` 비율 급증
- `reconcile fixed` 비율 급증(실시간 경로 이상 신호)
- `duplicate_blocked` 급증(보상 경로 이상 신호)

---

## 10) 롤백 전략

- Feature Flag로 단계적 적용
  - `mvp.incremental.enabled` (증분 경로)
  - `mvp.reconcile-only.enabled` (배치 역할)
- 문제 발생 시:
  1) 증분 경로 off
  2) 기존 시간 배치 full scan 임시 복귀
  3) 보상 idempotency 제약은 유지

원칙:
- 롤백 시에도 중복 지급 방지 정책은 절대 해제하지 않는다.

---

## 11) 구현 체크리스트

- [ ] `MvpCandidateComparator`/`MvpCandidateSnapshot` 추가
- [ ] Redis Lua compare-and-set 구현 및 어댑터 추가
- [ ] 이벤트 기반 후보 갱신 use case 추가
- [ ] `DailyMvpRepository` 조건부 update/upsert 구현
- [ ] `DailyMvpAggregationService` 역할 재정의(reconcile)
- [ ] `DailyMvpRewardService` idempotency 강화
- [ ] 캐시 None 정책 정리 및 evict 흐름 점검
- [ ] 메트릭/로그/알람 반영
- [ ] 동시성/장애/재시도 통합 테스트 통과

---

## 12) 예상 효과

- 대량 트래픽에서도 집계 비용이 입력 이벤트 수에 비례해 선형화된다.
- 동시성 경쟁으로 인한 MVP 역전/덮어쓰기 리스크를 실질적으로 제거한다.
- 보상 중복 지급을 애플리케이션 로직이 아닌 데이터 제약까지 포함해 강하게 차단한다.
- 운영 관점에서 장애 탐지와 복구 절차가 명확해진다.
