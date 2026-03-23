# PR: 멀티 로드뷰 Reissue CAS 멱등 처리 및 API 계약 강화

## 배경

로드뷰 라운드에서 특정 좌표가 프론트에서 로드되지 않을 때, 여러 클라이언트가 동시에 `reIssue`를 호출하면 같은 라운드에서 좌표가 연속 재발급되는 문제가 있었다.

기존 방식(`observedVersion -> lock -> currentVersion 비교`)은 "락 대기 중 버전 변경"은 일부 막았지만, 순차 중복 요청(수백 ms 시차)은 통과할 수 있어 라운드 좌표 분기가 재발생했다.

이번 PR은 reissue를 **CAS(compare-and-set) 기반 버전 멱등 처리**로 전환해, 동일 버전 요청에서 재발급 성공을 1회로 제한한다.

---

## 이번 PR 범위

- Reissue API 요청 계약 변경: `expectedRoundVersion` 필수
- 라운드 엔티티에 버전/재발급 메타 데이터 추가
- DB CAS 업데이트 쿼리 도입(동일 버전 1회 성공 보장)
- Reissue UseCase를 Redis 버전 의존에서 DB 버전 기반 판정으로 전환
- 응답에 `reissued` 플래그 추가(실제 재발급 수행 여부)
- 테스트/문서 갱신

---

## 변경 사항

## 1) Reissue API 계약 강화

대상 파일:
- `src/main/java/com/kospot/multi/game/presentation/dto/request/MultiGameRequest.java`
- `src/main/java/com/kospot/multi/game/presentation/controller/MultiRoadViewGameController.java`

핵심 변경:
- `MultiGameRequest.Reissue` 추가
  - `expectedRoundVersion` (`@NotNull`)
  - `reason` (optional)
- `POST /rooms/{roomId}/roadview/games/{gameId}/rounds/{roundId}/reIssue`가 body를 받도록 변경

요청 예시:
```json
{
  "expectedRoundVersion": 12,
  "reason": "ROADVIEW_LOAD_FAILED"
}
```

## 2) Reissue 응답 멱등 정보 추가

대상 파일:
- `src/main/java/com/kospot/multi/game/presentation/dto/response/MultiRoadViewGameResponse.java`

핵심 변경:
- `RoundProblem`에 `reissued` 필드 추가
  - `true`: 이번 요청이 실제 재발급 수행
  - `false`: 이미 다른 요청이 처리한 최신 상태 반환

## 3) 라운드 엔티티 버전 필드 추가

대상 파일:
- `src/main/java/com/kospot/multi/round/entity/RoadViewGameRound.java`

추가 필드:
- `roundVersion` (default `1`)
- `reissueCount` (default `0`)
- `lastReissueAt`

## 4) DB CAS 쿼리 도입

대상 파일:
- `src/main/java/com/kospot/multi/round/infrastructure/persistence/RoadViewGameRoundRepository.java`
- `src/main/java/com/kospot/multi/round/application/adaptor/RoadViewGameRoundAdaptor.java`
- `src/main/java/com/kospot/multi/round/application/service/roadview/RoundPreparationService.java`

핵심 변경:
- `tryAdvanceReissueVersion(...)` 추가
- 조건:
  - `roundVersion == expectedRoundVersion`
  - `isFinished = false`
  - `serverStartTime IS NULL` (phase guard)
  - `reissueCount < maxReissueCount`
  - `cooldown` 조건 충족
- 성공 행 수 `1`이면 유일 수행자, `0`이면 중복/차단으로 최신 상태 반환

## 5) Reissue UseCase 로직 전환

대상 파일:
- `src/main/java/com/kospot/multi/round/application/usecase/roadview/NextRoadViewRoundUseCase.java`

핵심 변경:
- `reissueRound(roomId, gameId, roundId, expectedRoundVersion)` 시그니처로 변경
- Redis `getRoundVersion/incrementRoundVersion` 기반 판정 제거
- CAS 성공 시만 재발급 + 브로드캐스트
- CAS 실패 시 최신 라운드 조회 후 `reissued=false` 응답
- `roomId-gameId-roundId` 소유권 검증 강화
- phase guard(`serverStartTime != null` 또는 `isFinished`) 시 `400` 처리

---

## 프론트 연동 포인트 (중요)

1. `reIssue` 호출 시 body에 `expectedRoundVersion` 필수 전달
2. 프론트 상태에 `roundVersion`을 단일 소스로 유지
3. 타이머 시작 이후에는 `reIssue` 호출 금지(현재 정책)
4. `reissued=false`도 실패가 아닌 정상 응답으로 처리

400 이슈 원인 정리:
- body 미전달(`expectedRoundVersion` 누락)
- 타이머 시작 후 reIssue 호출(phase guard 위반)

---

## 영향도

- Reissue API 계약 변경(프론트 수정 필요)
- 라운드 버전의 정합성 기준을 DB 중심으로 전환
- 기존 Redis version 기반 중복 방지 로직은 reissue 경로에서 제거

---

## 테스트/검증

대상 파일:
- `src/test/java/com/kospot/application/multi/round/roadview/NextRoadViewRoundUseCaseTest.java`

추가/수정 검증:
- CAS 실패 시 mutation 없이 최신 상태 반환(`reissued=false`)
- CAS 성공 시 1회 재발급 + 브로드캐스트(`reissued=true`)

로컬 제약:
- `./gradlew test` / `./gradlew compileJava` 실행 시 환경 이슈 발생
  - `Could not find or load main class worker.org.gradle.process.internal.worker.GradleWorkerMain`

---

## 커밋

- `f4a8c423` Implement CAS-based round reissue flow with versioned idempotency #193
- `f26011a7` Add CAS reissue test coverage and hardened rollout plan #193

---

## 관련 문서

- `docs/problem-solve/멀티_로드뷰_reissue_CAS_멱등_실행계획.md`

---

## 후속 작업 (Out of Scope)

- DB 마이그레이션 스크립트 확정/적용(`round_version`, `reissue_count`, `last_reissue_at`)
- Reissue 이벤트 `AFTER_COMMIT`/Outbox 전환
- 멀티 인스턴스 동시성 통합 테스트 및 운영 알람 튜닝
