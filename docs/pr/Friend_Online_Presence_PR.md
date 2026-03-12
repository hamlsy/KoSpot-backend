# PR: 친구 목록 온라인 상태 정확도 개선 (로비 기반 -> WebSocket 상태 기반)

## 배경

기존 `/friends` 온라인 판정은 `LobbyPresenceService`의 `lobby:users` 전체 스캔 결과에 의존했다. 이 방식은 두 가지 문제가 있었다.

1. 정확도 문제
- 로비 구독 상태와 실제 WebSocket 연결 상태가 다를 수 있다.
- subscribe join 경로가 주석 처리된 구간이 있어 온라인 누락 가능성이 있다.

2. 부하 문제
- 요청 1회마다 전체 온라인 세션 집합을 읽는 구조라, 친구 수와 무관하게 비용이 커진다.
- 즉, 필요한 것은 "내 친구 F명" 상태인데 실제로는 "전체 온라인 N명"을 읽는다.

이번 PR은 이를 **연결 상태 단일 소스 기준**으로 전환하고, 조회 비용을 친구 수 기준으로 줄이는 데 집중한다.

---

## 이번 PR 범위

- `/friends`의 `online` 산정 기준 전환
- 캐시 hit/miss와 관계없이 `online` 동적 overwrite
- Redis pipeline 기반 friend 대상 배치 조회 도입
- 테스트 및 API 문서 반영

대규모 아키텍처 분리(포트 계층 확장, feature flag, 캐시 키 마이그레이션)는 이번 PR 범위에서 제외했다.

---

## 변경 사항

## 1) 온라인 판정 소스 전환

대상 파일:
- `src/main/java/com/kospot/friend/application/usecase/GetMyFriendsUseCase.java`

핵심 변경:
- `LobbyPresenceService` 의존 제거
- `FriendOnlineStatusService`를 통해 friendMemberId 목록만 조회
- 캐시 데이터 사용 여부와 무관하게, 반환 직전 `online`을 재계산

정책:
- `state == CONNECTED`일 때만 `online=true`
- 그 외(`DISCONNECTED_TEMP`, `LEFT`, null)는 `false`

의도:
- API 의미를 명확히 하고 운영 해석을 단순화

## 2) Redis pipeline 기반 온라인 조회 서비스 추가

대상 파일:
- `src/main/java/com/kospot/friend/infrastructure/redis/service/FriendOnlineStatusService.java`

핵심 변경:
- 입력 friend ID 목록을 정제(null/중복/비정상 id 제거)
- `websocket:connection:state:member:{id}` hash의 `state` 필드를 pipeline으로 일괄 조회
- 예외 발생 시 fail-safe로 빈 set 반환(결과적으로 offline 처리)

효과:
- 기존 `O(N)`(전체 온라인 세션 기반)에서 `O(F)`(친구 수 기반)로 전환
- 대규모 동시접속 시간대에도 `/friends` 비용이 더 예측 가능해짐

## 3) 캐시 전략 (이번 PR)

대상 파일:
- `src/main/java/com/kospot/friend/application/usecase/GetMyFriendsUseCase.java`

적용 방식:
- 기존 friend list 캐시는 유지
- 캐시된 `online`은 신뢰하지 않고, 응답 직전 매번 overwrite

왜 이렇게 했는가:
- 캐시 키 버전 마이그레이션 없이 즉시 효과를 얻기 위한 실용적 선택
- 요구사항(친구 온라인 정확도 개선)에 필요한 최소 변경

## 4) 테스트 추가

대상 파일:
- `src/test/java/com/kospot/application/friend/GetMyFriendsUseCaseTest.java`

검증 시나리오:
- cache hit에서도 온라인 값 overwrite 되는지 검증
- cache miss 시 정적 데이터 캐시 저장 + 온라인 동적 계산 검증

## 5) API 문서 반영

대상 파일:
- `docs/api/friend/README.md`

변경 내용:
- `online` 의미를 "WebSocket 연결 상태가 `CONNECTED`인지"로 명시

---

## 왜 이 변경이 핵심인가

- 정확도: 로비 참여 여부가 아닌 실제 연결 상태 기준으로 판정한다.
- 성능: 친구 API 비용이 전체 온라인 규모에 끌려가지 않는다.
- 안정성: Redis 조회 실패 시에도 API 전체 실패 대신 offline fallback으로 안전 동작한다.
- 범위 통제: 목적 달성에 불필요한 대공사를 피했다.

---

## 부하/효율 관점 정리

기존:
- 온라인 판정 데이터 소스: `lobby:users` 전체
- 비용 기준: 전체 온라인 세션 수 `N`

변경 후:
- 온라인 판정 데이터 소스: member별 connection state key
- 비용 기준: 친구 수 `F`

해석:
- 일반적으로 `F << N`이므로 friend 조회 경로 효율이 개선된다.
- pipeline 적용으로 RTT를 줄여 Redis 왕복 비용을 완화한다.

---

## 장애/예외 처리 정책

- presence 조회 Redis 예외 발생 시: 빈 결과 반환 -> `online=false`
- 파싱/누락 상태: offline 처리

운영 원칙:
- 친구 목록 자체는 최대한 정상 응답
- online 필드만 보수적으로 fallback

---

## 테스트/검증

완료:
- 유즈케이스 단위 테스트 추가(캐시 hit/miss 경로)

환경 제약:
- 로컬 Gradle 컴파일 환경 이슈로 테스트 실행 검증은 실패
- 에러: `Could not find or load main class worker.org.gradle.process.internal.worker.GradleWorkerMain`

권장 추가 검증:
- stage에서 `/friends` p95 및 Redis ops 비교
- connect/disconnect 직후 online 반영 확인
- 동시접속 많은 시간대에서 응답시간 추적

---

## 커밋

- `687de307` docs: tighten friend online presence execution plan
- `b430055f` feat(friend): compute online status from websocket connection state
- `764eaead` test(friend): verify online overwrite and document status semantics

---

## Out of Scope (후속 가능)

- `DISCONNECTED_TEMP`를 online으로 간주하는 정책 토글화
- friend 도메인 presence 조회 포트/어댑터 계층 고도화
- 캐시 스키마 정적/동적 필드 완전 분리 및 키 버전 업
