# Daily MVP 동시성 안정화 작업용 세부 개발 태스크

## 0) 운영 원칙 (작업 시작 전 합의)
- 모든 변경은 Feature Flag 하에서 점진 적용한다.
- 동시성 안전성은 "락 추가"보다 "원자 비교-갱신 + 조건부 업데이트"를 우선한다.
- 보상 중복 방지는 애플리케이션 로직과 DB 제약의 이중 방어로 구현한다.
- 각 태스크는 단위 테스트 또는 통합 테스트를 동반한다.

---

## 1) 에픽 분해

### Epic A. 도메인 정책/모델 정리 (P0)
- A-1: MVP 비교 정책 객체 도입
- A-2: 후보 스냅샷 VO 도입
- A-3: 기존 집계 서비스에 정책 적용(기존 동작 동일 보장)

### Epic B. 증분 집계 경로 도입 (P1)
- B-1: 게임 종료 이벤트 발행/구독 경로 구성
- B-2: Redis Lua compare-and-set 구현
- B-3: 후보 교체 성공 시 DB 조건부 반영
- B-4: 캐시 무효화 연결

### Epic C. 배치 역할 전환 (P2)
- C-1: full-scan hourly 집계를 reconcile 배치로 변경
- C-2: Redis/DB 불일치 탐지/복구 로직 추가

### Epic D. 보상 경로 idempotency 강화 (P0~P2 병행)
- D-1: idempotency key 정책 도입
- D-2: 포인트 이력/보상 중복 방지 제약 보강

### Epic E. 관측성/운영/롤백 (전 단계)
- E-1: 메트릭 추가
- E-2: 구조화 로그 추가
- E-3: 런북/롤백 절차 문서화

---

## 2) 태스크 보드 (실작업 단위)

## A-1. MVP 비교 정책 객체 도입
**목표**
- 점수 비교 규칙을 서비스 로직에서 분리하여 테스트 가능하게 만든다.

**변경 대상(예상)**
- `src/main/java/com/kospot/mvp/domain/*` 하위 신규 클래스
  - `MvpCandidateComparator`

**구현 포인트**
- 우선순위: `score DESC` -> `endedAt ASC` -> `id ASC`
- null-safe 처리 기준 정의(endedAt null 불가 정책 권장)

**완료 조건**
- 비교 규칙 단위 테스트 통과(동점/역전/경계 케이스 포함)

**예상 난이도/공수**
- S / 0.5d

---

## A-2. 후보 스냅샷 VO 도입
**목표**
- 후보 비교/저장에 필요한 필드를 단일 객체로 캡슐화한다.

**변경 대상(예상)**
- `src/main/java/com/kospot/mvp/domain/*` 하위 신규 클래스
  - `MvpCandidateSnapshot`

**구현 포인트**
- `memberId`, `roadViewGameId`, `score`, `endedAt`, `rankTier`, `rankLevel`, `ratingScore`
- 생성 팩토리: `RoadViewGame + GameRank` 기반 변환 메서드 제공

**완료 조건**
- 불변 객체(immutable) 형태 및 생성 검증 로직 확보

**예상 난이도/공수**
- S / 0.5d

---

## A-3. 기존 집계 경로 정책 연동
**목표**
- 기존 `DailyMvpAggregationService`에서 정책 객체를 사용하도록 리팩터링한다.

**변경 대상(예상)**
- `src/main/java/com/kospot/mvp/application/service/DailyMvpAggregationService.java`

**구현 포인트**
- 기존 동작 동일성 유지(기능 변경 없음)
- private 메서드 분리로 가독성 개선

**완료 조건**
- 리팩터링 전후 회귀 테스트 통과

**예상 난이도/공수**
- M / 0.5d

---

## B-1. 게임 종료 이벤트 기반 증분 집계 트리거
**목표**
- RANK + COMPLETED 게임 종료 시 후보 갱신 파이프라인을 시작한다.

**변경 대상(예상)**
- 게임 완료 처리 use case/service
- `src/main/java/com/kospot/mvp/application/*` 하위 이벤트 리스너/유즈케이스 신규

**구현 포인트**
- 이벤트 payload 최소화: `gameId`, `memberId`, `endedAt`, `score`, `date`
- 트랜잭션 후행 발행(커밋 후 발행) 원칙 준수

**완료 조건**
- 게임 완료 시 이벤트 발행/수신 확인

**예상 난이도/공수**
- M / 1d

---

## B-2. Redis Lua compare-and-set 구현
**목표**
- 분산 환경에서도 후보 비교-교체를 단일 원자 연산으로 보장한다.

**변경 대상(예상)**
- `src/main/java/com/kospot/mvp/infrastructure/redis/*` 신규/확장
  - Redis repository/adaptor
  - Lua script 로딩/실행 컴포넌트

**구현 포인트**
- 키: `mvp:daily:candidate:{yyyy-MM-dd}`
- 반환값 계약: `1=교체`, `0=유지`, `-1=오류(또는 예외)`
- 값 저장 포맷 고정(필드명 스키마 문서화)

**완료 조건**
- 동시 100+ 호출에서 원자성 보장(통합 테스트)

**예상 난이도/공수**
- M~L / 1.5d

---

## B-3. DB 조건부 반영(upsert/conditional update)
**목표**
- Redis 교체 성공 시에만 DB를 갱신하고, 우수 후보만 반영한다.

**변경 대상(예상)**
- `src/main/java/com/kospot/mvp/infrastructure/persistence/DailyMvpRepository.java`
- `src/main/java/com/kospot/mvp/application/adaptor/DailyMvpAdaptor.java`
- `src/main/java/com/kospot/mvp/application/service/*` 신규 반영 유즈케이스

**구현 포인트**
- 조건부 update 쿼리 도입
- row 없음 시 insert, unique 충돌 시 제한 재시도
- 재시도 횟수/백오프 상수화

**완료 조건**
- 역전 덮어쓰기 0건
- unique 충돌 시에도 최종 상태 일관성 확보

**예상 난이도/공수**
- L / 1.5d

---

## B-4. 조회 캐시 무효화 연결
**목표**
- 후보 교체 시 `GET /mvps` 결과가 즉시 최신화되도록 캐시 전략 연결.

**변경 대상(예상)**
- `src/main/java/com/kospot/mvp/infrastructure/redis/service/DailyMvpCacheService.java`
- 후보 갱신 유즈케이스

**구현 포인트**
- 후보 교체 성공 시 `evict(date)`
- None 캐시 존재 시 삭제 정책 명확화

**완료 조건**
- 후보 교체 직후 조회 시 stale 데이터 미노출

**예상 난이도/공수**
- S / 0.5d

---

## C-1. hourly 집계 -> reconcile 배치 전환
**목표**
- 기존 full scan 배치를 정합성 검증/복구 배치로 축소한다.

**변경 대상(예상)**
- `src/main/java/com/kospot/mvp/application/scheduler/DailyMvpScheduler.java`
- `src/main/java/com/kospot/mvp/application/service/DailyMvpAggregationService.java`

**구현 포인트**
- 스케줄 락은 유지
- reconcile 대상 날짜 범위 정책(오늘/어제) 명시
- fallback full scan 경로는 flag로만 허용

**완료 조건**
- 운영 로그에서 hourly full scan 제거
- 불일치 발생 시 자동 복구 확인

**예상 난이도/공수**
- M / 1d

---

## C-2. 불일치 탐지/복구 로직
**목표**
- Redis 후보와 DB snapshot의 불일치를 탐지하고 안전하게 수렴시킨다.

**변경 대상(예상)**
- `src/main/java/com/kospot/mvp/application/service/*` 신규 reconcile service

**구현 포인트**
- 비교 기준 동일화(정책 객체 재사용)
- 복구 작업 idempotent 보장

**완료 조건**
- 재실행해도 동일 결과(no-op) 보장

**예상 난이도/공수**
- M / 1d

---

## D-1. 보상 idempotency key 도입
**목표**
- 보상 배치 재실행에도 동일 보상이 중복 지급되지 않게 한다.

**변경 대상(예상)**
- `src/main/java/com/kospot/mvp/application/service/DailyMvpRewardService.java`
- 포인트 적립/이력 저장 경로

**구현 포인트**
- key 예시: `mvp-reward:{mvpDate}:{memberId}`
- 처리 순서: lock -> 이미 처리 확인 -> 지급 -> 처리 완료 마크

**완료 조건**
- 배치 중복 실행/재시도 시 중복 지급 0건

**예상 난이도/공수**
- M / 1d

---

## D-2. DB 유니크 제약 보강
**목표**
- 애플리케이션 버그/재시도 오류가 있어도 DB 차원에서 중복 지급을 차단한다.

**변경 대상(예상)**
- 포인트 이력 엔티티/테이블 마이그레이션
- 관련 repository 예외 처리

**구현 포인트**
- 자연키 후보 검토: `(member_id, point_history_type, ref_date)` 또는 동등 키
- 마이그레이션 전 기존 데이터 중복 여부 점검 스크립트 필요

**완료 조건**
- 중복 insert 시도 시 unique constraint로 차단 및 안전 처리

**예상 난이도/공수**
- M~L / 1.5d

---

## E-1. 메트릭 추가
**목표**
- 운영에서 동작/장애/성능을 계량적으로 확인한다.

**구현 포인트**
- `mvp_candidate_update_total{result}`
- `mvp_candidate_conflict_retry_total`
- `mvp_reconcile_total{result}`
- `mvp_reward_total{result}`

**완료 조건**
- 대시보드에서 단계별 성공/실패 추세 확인 가능

**예상 난이도/공수**
- S / 0.5d

---

## E-2. 구조화 로그
**목표**
- 사건 단위 추적 가능성 확보.

**구현 포인트**
- 필수 필드: `mvpDate`, `gameId`, `memberId`, `score`, `endedAt`, `compareResult`, `instanceId`
- 실패 로그는 원인 코드 분류(REDIS_FAIL/DB_CONFLICT/VALIDATION_FAIL)

**완료 조건**
- 단일 gameId 기준 end-to-end 추적 가능

**예상 난이도/공수**
- S / 0.5d

---

## E-3. 롤아웃/롤백 런북
**목표**
- 장애 시 운영자가 빠르게 안전한 상태로 전환할 수 있게 한다.

**구현 포인트**
- 플래그 전환 순서, 모니터링 확인 포인트, 롤백 단계 문서화

**완료 조건**
- 온콜 엔지니어가 문서만으로 10분 내 대응 가능

**예상 난이도/공수**
- S / 0.5d

---

## 3) 권장 작업 순서 (의존성 기준)
1. A-1 -> A-2 -> A-3
2. D-1(보상 안전장치 선적용)
3. B-1 -> B-2 -> B-3 -> B-4
4. C-1 -> C-2
5. D-2(마이그레이션 포함), E-1 -> E-2 -> E-3

총 예상: 약 8~10 영업일(리뷰/QA 포함)

---

## 4) 브랜치/PR 분할 권장

### PR-1 (P0)
- A-1, A-2, A-3, D-1, E-1 일부
- 목표: 정책 분리 + 보상 중복 방지 1차 완료

### PR-2 (P1)
- B-1, B-2, B-3, B-4
- 목표: 증분 집계 실사용 가능 상태

### PR-3 (P2)
- C-1, C-2, E-2, E-3
- 목표: 배치 역할 전환 + 운영 안정화

### PR-4 (선택)
- D-2 (DB 제약/마이그레이션)
- 목표: 최종 데이터 계층 안전장치 확정

---

## 5) 리뷰 체크포인트 (클린 코드 기준)
- 비즈니스 규칙이 서비스 메서드 안에 흩어져 있지 않은가?
- 동시성 제어가 코드 주석이 아니라 원자 연산/조건부 쿼리로 구현되었는가?
- 재시도 정책이 무한 루프 없이 제한되고 관측 가능한가?
- 에러 처리에서 fail-open이 아닌 fail-safe 원칙을 지켰는가?
- 테스트가 "정상 흐름"뿐 아니라 "경쟁/장애 흐름"을 포함하는가?

---

## 6) QA 시나리오 체크리스트
- [ ] 동시 이벤트에서 최종 후보 불변성 검증
- [ ] out-of-order 이벤트 역전 방지 검증
- [ ] 보상 배치 재실행 중복 지급 방지 검증
- [ ] Redis 장애 후 복구 배치 정합성 회복 검증
- [ ] Feature Flag on/off 시 동작 일관성 검증
