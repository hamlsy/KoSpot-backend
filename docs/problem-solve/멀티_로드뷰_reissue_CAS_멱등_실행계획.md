# 멀티 로드뷰 Reissue CAS 멱등 처리 실행 계획

## 1) 목적

로드뷰 라운드에서 좌표 로딩 실패 시 여러 클라이언트가 동시에 `reIssue`를 호출해도,
동일 라운드/동일 버전 기준으로 **반드시 1회만 재발급**되도록 보장한다.

핵심 목표:

- 동일 `expectedRoundVersion`에서 재발급 성공은 1건만 허용
- 나머지 중복 요청은 실패가 아니라 동일 최신 상태를 반환(멱등 응답)
- 새 버전 좌표도 로딩 실패하면 다음 버전으로 재재발급 허용
- 운영 안전장치(최대 횟수, 쿨다운, 관측 지표) 포함

추가 목표(보완):

- `roundVersion`의 정합성 기준 저장소를 단일화(DB canonical)
- 라운드 phase(허용 구간) 가드로 타이머/제출과 충돌 차단
- 멀티 인스턴스/브로드캐스트 실패 상황까지 포함한 운영 안정성 확보

---

## 2) 현재 문제 진단 요약

현재 구현은 `observedVersion -> DB lock 획득 -> currentVersion 비교` 방식으로
"락 대기 중 버전 변경"은 막지만, **순차 중복 요청**은 완전히 차단하지 못한다.

대표 시나리오:

1. A가 reissue 성공(버전 10 -> 11)
2. 직후 B가 요청 시작(이미 11을 보고 시작)
3. B도 조건 통과해 다시 재발급(11 -> 12)

결과: 같은 라운드에서 플레이어마다 서로 다른 좌표를 볼 수 있다.

---

## 3) 목표 동작(정책)

### 3.1 멱등 정책

- **같은 버전에서 중복 재발급 금지**
- **다음 버전 재발급 허용**

예시:

- `expectedVersion=10` 요청 N개 동시 도착 -> 1개만 성공, 전체는 최종적으로 `version=11` 상태 수렴
- `version=11` 좌표도 실패 -> `expectedVersion=11`로 다시 요청 가능(다시 1개만 성공, `version=12`)

### 3.2 API 응답 정책

중복 요청도 200으로 정상 응답(클라이언트 단순화):

- `reissued=true`: 이번 요청이 실제 좌표 교체를 수행
- `reissued=false`: 이미 다른 요청이 처리한 최신 상태 반환

---

## 4) 설계 원칙

1. **정합성의 근거는 락이 아닌 CAS(Compare-And-Set)**
2. 락은 선택적 보조 장치(부하 완화용)
3. 브로드캐스트는 "실제 재발급 수행자"만 1회 전송
4. 실패/재시도/중복 호출을 기본 전제로 설계
5. **`roundVersion`의 canonical source는 DB 하나만 사용**
6. **Reissue는 허용된 라운드 phase에서만 처리**

---

## 5) 권장 아키텍처

## 5.1 서버-클라이언트 계약

`POST /rooms/{roomId}/roadview/games/{gameId}/rounds/{roundId}/reIssue`

Request Body:

```json
{
  "expectedRoundVersion": 11,
  "reason": "ROADVIEW_LOAD_FAILED"
}
```

Response Body:

```json
{
  "gameId": 123,
  "roundId": 456,
  "roundVersion": 12,
  "reissued": true,
  "poiName": "...",
  "targetLat": 37.123,
  "targetLng": 127.123
}
```

중복 요청 응답 예시:

```json
{
  "gameId": 123,
  "roundId": 456,
  "roundVersion": 12,
  "reissued": false,
  "poiName": "...",
  "targetLat": 37.123,
  "targetLng": 127.123
}
```

## 5.2 CAS 저장소 선택

우선순위 권장:

1. **DB CAS(권장)**: 영속 상태와 강한 일관성 확보가 쉬움
2. Redis Lua CAS: 초저지연은 유리하나 DB 반영/복구 경계 관리가 더 복잡

본 계획은 DB CAS 기준으로 작성한다.

보완 원칙:

- `road_view_game_round.round_version`을 단일 진실 원천(SSOT)으로 사용
- Redis의 version 값은 캐시 또는 조회 최적화 용도로만 사용
- Redis 캐시가 존재하더라도 판정(성공/중복)은 반드시 DB CAS 결과로 결정

---

## 6) DB 기반 CAS 상세 설계

## 6.1 스키마

`road_view_game_round`에 다음 컬럼 추가:

- `round_version BIGINT NOT NULL DEFAULT 1`
- `reissue_count INT NOT NULL DEFAULT 0`
- `last_reissue_at TIMESTAMP NULL`

초기 라운드 생성 시 `round_version=1`.

마이그레이션 주의:

- 기존 데이터 백필: `round_version` null/0 데이터가 없도록 일괄 정리
- 인덱스 검토: `(id, multi_road_view_game_id, round_version)` 조건 최적화 확인

## 6.2 원자적 상태 전이

핵심 쿼리(개념):

```sql
UPDATE road_view_game_round
SET round_version = round_version + 1,
    reissue_count = reissue_count + 1,
    last_reissue_at = NOW()
WHERE id = :roundId
  AND multi_road_view_game_id = :gameId
  AND round_version = :expectedVersion
  AND is_finished = false
  AND reissue_count < :maxReissueCount
  AND (
      last_reissue_at IS NULL
      OR /* DB별 시간차 계산식 */ elapsed_ms(last_reissue_at, :now) >= :cooldownMs
  );
```

주의:

- 위 시간 비교는 DB 방언별 문법 차이가 크므로, 실제 구현은 다음 중 하나로 고정한다.
  - 애플리케이션에서 `now` 전달 + epoch 기반 비교
  - DB별 전용 함수 사용(MySQL/PG 각각 분리)
- 쿨다운 조건은 성능/이식성 관점에서 애플리케이션 검증 + CAS 보조 조건 조합을 권장

판정:

- 업데이트 행 수 `1`: CAS 성공(이번 요청이 유일한 재발급 수행자)
- 업데이트 행 수 `0`: CAS 실패(이미 처리됨/조건 불만족) -> 최신 상태 조회 후 멱등 반환

## 6.3 좌표 교체 절차

CAS 성공 요청만 실행:

1. 기존 좌표 invalidate
2. 새 좌표 할당
3. 라운드 상태 리셋(필요 시)
4. DB flush
5. 브로드캐스트 1회

중요:

- 좌표 invalidate와 새 좌표 할당은 같은 트랜잭션 경계에서 처리
- 좌표 뽑기 실패 시 트랜잭션 롤백으로 version/reissue_count 증가도 함께 롤백

추가 보완:

- `invalidateCoordinate`가 외부 시스템/별도 저장소라면 DB 트랜잭션과 원자성이 깨질 수 있다.
- 이 경우 전략을 분리한다.
  - 정합성 필수 경로: "새 좌표 할당 + round_version 반영"만 트랜잭션 내 보장
  - invalidate는 AFTER_COMMIT 비동기 처리(실패 시 재시도) 또는 outbox 소비로 처리

## 6.4 Reissue 허용 phase 가드

Reissue는 다음 조건을 모두 만족할 때만 허용한다.

- 해당 라운드가 `isFinished=false`
- 라운드 타이머 시작 전(권장) 또는 정책상 허용한 window 내
- 제출이 시작된 이후에는 기본 거부(정책 필요 시 예외 허용)

권장 정책:

- 기본: `LOADING/INTRO` 구간만 허용
- `IN_PROGRESS`에서는 거부(좌표 교체로 점수 정합성 깨짐 방지)

## 6.5 브로드캐스트 정합성

권장:

- 단기: 커밋 후 브로드캐스트(`@TransactionalEventListener(AFTER_COMMIT)`)
- 중장기: Outbox 패턴 적용(브로드캐스트 실패 자동 재시도)

명확화:

- AFTER_COMMIT만으로는 네트워크 실패까지 포함한 exactly-once를 보장하지 않는다.
- 수신자 멱등 처리를 위해 payload에 `roundId + roundVersion`을 포함하고, 클라이언트/서버 모두 중복 수신 안전하게 처리한다.

---

## 7) 서버 로직 의사코드

```text
reissueRound(roomId, gameId, roundId, expectedVersion):
  require expectedVersion != null

  validate roomId-gameId ownership
  validate roundId belongs to gameId
  validate phase guard (LOADING/INTRO only)

  casUpdated = roundRepository.tryAdvanceReissueVersion(
      roundId, gameId, expectedVersion, maxCount, cooldownMs)

  if casUpdated == 0:
      latest = roundRepository.findByIdFetchCoordinate(roundId)
      return RoundProblem(latest, reissued=false)

  // CAS 성공한 유일 요청
  round = roundRepository.findByIdFetchCoordinateForUpdate(roundId)
  reassignCoordinate(round)
  save/flush

  emit RoundReissuedEvent(roomId, roundId, round.roundVersion)
  return RoundProblem(round, reissued=true)
```

---

## 8) 도메인/코드 변경 항목

## 8.1 DTO/컨트롤러

- `MultiGameRequest.Reissue` 추가: `expectedRoundVersion`, `reason`
- `MultiRoadViewGameController.reissueRound(...)`에 body 파라미터 추가
- 응답 DTO `RoundProblem`에 `reissued` 필드 추가

## 8.2 Repository/Adaptor

- `RoadViewGameRoundRepository`에 CAS update 메서드 추가
- `roomId` 정합성 검증 메서드 추가(게임 소유 방 검증)
- `roundId -> gameId -> roomId` 3자 정합성 검증 쿼리/조회 추가

## 8.3 UseCase

- 기존 `observed/current version` 비교 로직 제거
- CAS 성공/실패 분기 로직으로 대체
- 브로드캐스트는 `reissued=true`일 때만 수행
- `expectedRoundVersion` 미전달 요청은 400 처리(호환기간 외)
- reissue phase 가드 추가(불가 상태는 명시 코드 반환)

## 8.4 Redis 사용 범위 재정의

- `roundVersion`의 정합성 기준을 DB로 단일화
- Redis version은 캐시/조회 최적화 목적으로만 사용(선택)
- 기존 `reissue lock`은 정합성 장치가 아닌 트래픽 완충 장치로 격하

추가 보완:

- `executeInitial/executeNextRound`에서 version 관리가 Redis 중심이라면 DB 기반으로 정렬 필요
- 최소한 reissue 경로에서는 Redis version 판정을 완전히 제거

---

## 9) 운영 안전장치

필수 설정값:

- `multi.roadview.reissue.max-count-per-round` (기본 5)
- `multi.roadview.reissue.cooldown-ms` (기본 1000)

초과 시 동작:

- 재발급 거절 + 현재 문제 반환 (`reissued=false`)
- 서버 로그/메트릭 경고 기록

---

## 10) 테스트 전략

## 10.1 단위 테스트

- 동일 `expectedVersion` 동시 요청 N개 -> 1개만 `reissued=true`
- 순차 중복 요청(짧은 시간차) -> 첫 요청만 실제 재발급
- 새 버전에서 재요청 -> 다시 1개만 성공
- max-count/cooldown 경계값 테스트

## 10.2 통합 테스트

- 멀티스레드(2/5/10 클라이언트)로 reissue 폭주 재현
- 최종적으로 모든 응답이 동일 `roundVersion`과 동일 좌표로 수렴하는지 검증
- 브로드캐스트 이벤트가 버전당 정확히 1회인지 검증

## 10.3 멀티 인스턴스 테스트

- 애플리케이션 인스턴스 2개 이상에서 동일 라운드 동시 reissue 검증
- 서로 다른 인스턴스에서 요청을 받아도 버전당 1회 재발급만 발생하는지 확인
- 인스턴스 한쪽 브로드캐스트 실패 시 outbox/재시도로 복구되는지 확인

## 10.4 회귀 테스트

- 기존 라운드 시작/종료/제출 흐름 영향도 확인
- 게임 취소/타임아웃 시나리오와 충돌 없는지 확인

---

## 11) 관측(Observability)

로그 키:

- `reissue.request` (roomId, gameId, roundId, expectedVersion)
- `reissue.cas.success` / `reissue.cas.duplicate`
- `reissue.broadcast.sent`
- `reissue.blocked.max_count` / `reissue.blocked.cooldown`

메트릭:

- `reissue_requests_total`
- `reissue_success_total`
- `reissue_duplicate_total`
- `reissue_blocked_total{reason=max_count|cooldown}`
- `reissue_broadcast_total`

알람 기준(예시):

- `duplicate/requests > 0.7` 5분 지속
- `blocked_total` 급증

---

## 12) 배포/마이그레이션 절차

1. DB 마이그레이션 반영(`round_version`, `reissue_count`, `last_reissue_at`)
2. 애플리케이션 배포(신규 API 계약 포함, feature flag OFF)
3. 프론트 배포(`expectedRoundVersion` 전달)
4. feature flag ON(점진적 트래픽)
5. 지표/로그 모니터링

권장 전환 단계:

- Stage A: 기존 로직 유지 + 신규 필드 로깅만
- Stage B: 예상 버전 전달 클라이언트 비율 확인
- Stage C: CAS 경로 활성화(일부 방/일부 트래픽)
- Stage D: 전체 전환 후 구경로 제거

롤백 전략:

- 기능 플래그 Off로 즉시 이전 경로 복귀
- 스키마 컬럼은 하위호환 유지(읽기 영향 없음)

---

## 13) 리스크와 대응

- **프론트 미준수(버전 누락/오래된 버전)**: 서버에서 최신 상태 멱등 반환, 로그 경고
- **좌표 풀 고갈**: 재발급 실패 시 명시 에러 코드 + 라운드 중단/대체 정책 필요
- **브로드캐스트 누락**: AFTER_COMMIT 이벤트 또는 Outbox로 복구
- **DB/Redis 버전 불일치**: DB를 정합성 기준으로 고정하고 Redis는 재구축 가능 캐시로 취급
- **IN_PROGRESS 중 재발급 호출**: phase 가드로 차단하고 운영 알람 발송

---

## 14) 완료 기준(Definition of Done)

- 동일 버전 중복 요청에서 실제 좌표 변경이 1회로 제한됨
- 순차 중복 요청에서도 추가 좌표 변경이 발생하지 않음
- 새 버전 재발급은 정상 허용됨
- 라운드별 브로드캐스트가 버전당 1회만 발생
- 멀티 인스턴스 환경에서도 위 조건이 동일하게 보장됨
- 단위/통합 테스트 및 운영 지표로 재현 불가 확인

---

## 15) 즉시 실행 체크리스트

- [ ] Reissue 요청 DTO에 `expectedRoundVersion` 추가
- [ ] `RoundProblem`에 `reissued` 추가
- [ ] `RoadViewGameRound` 버전/카운트/시간 컬럼 추가
- [ ] Repository CAS update 구현
- [ ] UseCase를 CAS 분기로 교체
- [ ] `expectedRoundVersion` 필수 검증 및 에러 코드 정리
- [ ] reissue phase 가드(LOADING/INTRO only) 적용
- [ ] roomId-gameId 정합성 검증 추가
- [ ] roundId-gameId-roomId 3자 정합성 검증 추가
- [ ] 동시성/순차 중복 테스트 추가
- [ ] 멀티 인스턴스 동시성 테스트 추가
- [ ] 메트릭/로그/알람 추가
- [ ] feature flag 적용
