# 멀티 로드뷰 라운드 재발행(reIssue) 동시성 문제 해결 문서

## 문제 상황

### 1) 장애 시나리오
- 엔드포인트: `POST /rooms/{roomId}/roadview/games/{gameId}/rounds/{roundId}/reIssue`
- 호출 경로: `MultiRoadViewGameController.reissueRound` -> `NextRoadViewRoundUseCase.reissueRound`
- 실제 이슈 트리거:
  1. 특정 좌표가 클라이언트에서 로드되지 않음
  2. 같은 라운드의 여러 플레이어가 거의 동시에 `reIssue` API 호출
  3. 동시 호출 처리 중 라운드 상태와 브로드캐스트 메시지의 정합성 붕괴

### 2) 기존 구현의 구조적 문제
- 기존 `NextRoadViewRoundUseCase.reissueRound`는 Redis 락 획득 실패 분기에서도 `roundPreparationService.reissueRound(...)`를 호출하여 실제 좌표를 다시 바꾸고 있었다.
- 반면 `roundVersion` 증가와 `broadcastRoundStart`는 락 획득 성공 분기에서만 수행되었다.
- 결과적으로 다음 문제가 동시에 발생 가능했다.
  - 좌표는 여러 번 바뀌는데 버전/방송은 1회만 발생
  - 플레이어별로 받은 문제 정보와 서버 최종 DB 상태가 달라짐
  - "누가 마지막으로 커밋했는가"에 따라 최종 좌표가 달라지는 race condition 발생

### 3) 왜 단순 직렬화만으로는 부족한가
- 비관적 락만 도입하면 동시 실행은 직렬화되지만, 대기 요청이 순서대로 다시 재발행을 수행할 수 있다.
- 즉, A 요청 완료 후 대기하던 B 요청이 이어서 또 재발행하면 브로드캐스트가 반복될 수 있다.
- 동시에, 실제 요구사항은 "다음 재발행 자체를 영구 금지"가 아니라 "같은 시점에 몰린 중복 호출만 무해화"이므로 영구 차단 전략은 부적합하다.

---

## 해결 과정

### 1) 설계 원칙
- Redis 분산 락 기반 제어를 제거하고, DB 비관적 락으로 동시성 제어를 통일한다.
- 재시도 로직은 도입하지 않는다.
- 같은 시점에 몰린 중복 호출은 no-op 처리하되, 이후 새로운 실패 상황에서의 재재발행은 허용한다.

### 2) 핵심 전략: "비관적 락 + 버전 기반 no-op 게이트"
- `RoadViewGameRound` 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 재발행 판단을 수행한다.
- 요청 시작 시점의 `observedVersion`과 락 획득 후 `currentVersion`을 비교한다.
  - `currentVersion != observedVersion`이면 이미 선행 요청이 재발행을 완료한 상태이므로 no-op 반환
  - `currentVersion == observedVersion`이면 이번 요청이 실질 재발행 수행
- 이 방식의 장점:
  - 동시 요청은 1회만 실질 재발행/브로드캐스트
  - 후속 정상 재발행(새 버전 기준)은 허용
  - 별도 재시도 루프 없이도 사용자 체감 안정성 확보

### 3) 코드 변경 포인트
- Repository
  - `RoadViewGameRoundRepository`에 `findByIdFetchGameForUpdate` 추가
  - `@Lock(LockModeType.PESSIMISTIC_WRITE)` 적용
- Adaptor
  - `RoadViewGameRoundAdaptor`에 `queryByIdFetchGameForUpdate` 추가
- Service
  - `RoundPreparationService`에 락 기반 조회 메서드 추가
  - 잠금된 라운드를 받아 재발행하는 오버로드 메서드 추가
- UseCase
  - `NextRoadViewRoundUseCase.reissueRound`에서 Redis 락 획득/해제 로직 제거
  - 버전 비교 기반 no-op 게이트 도입
  - 실질 재발행 경로에서만 버전 증가 + 브로드캐스트 수행

### 4) 기대 불변식
- 문제 변경(좌표 변경) -> 버전 증가 -> 브로드캐스트가 항상 같은 실행 경로에서만 발생
- 같은 버전 기준 중복 요청은 상태 변경 없이 동일 최신 문제를 반환
- 이후 새로운 실패 상황에서 재요청 시(새 버전 관측) 정상 재발행 가능

---

## 결과

### 1) 기능 결과(목표)
- 같은 시점 동시 호출에 대해 "실질 재발행 1회 + 브로드캐스트 1회" 보장
- 중복 호출은 오류 없이 no-op 응답
- 재발행된 좌표가 다시 실패한 경우, 새 버전 기준으로 추가 재발행 허용

### 2) 운영 관점 기대 효과
- 플레이어 간 라운드 문제 불일치 감소
- 브로드캐스트/버전/DB 상태 간 정합성 회복
- Redis 락 TTL 만료/소유권 문제와 같은 운영 리스크 축소

### 3) 검증 항목
- 동시 요청 N건(동일 roundId)에서
  - DB 좌표 변경 횟수
  - 버전 증가 횟수
  - `broadcastRoundStart` 송신 횟수
  - 각 요청 응답의 좌표/버전 일치 여부
- 재재발행 시나리오(새 버전 관측 후 재호출) 정상 동작 여부

### 4) 비고
- 본 문서는 구현 전에 수립한 해결 기준이며, 구현/검증 완료 후 실제 측정 결과를 동일 문서에 업데이트한다.
