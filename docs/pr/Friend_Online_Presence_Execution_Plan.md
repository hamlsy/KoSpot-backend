# 친구 목록 온라인 상태 정확도 개선 실행 계획 (현실성 점검 반영)

## 0) 배경과 목표

친구 목록 API(`/friends`)의 `online` 값이 실제 접속 상태와 어긋날 수 있다. 원인은 온라인 판정 소스와 캐시 방식이다.

목표:
- 온라인 판정을 WebSocket 연결 상태 기준으로 통일
- DB/Redis/서버 부하를 늘리지 않는 선에서 정확도 개선
- 과도한 구조 변경 없이 빠르게 배포 가능한 계획 수립

---

## 1) 냉정한 부하 분석 (현재 vs 개선안)

### 1-1. 현재 방식의 낭비 지점

현재 `GetMyFriendsUseCase`는 `LobbyPresenceService.getOnlineMemberIds()`를 호출하며, 내부에서 `lobby:users` 전체 엔트리를 읽는다.

의미:
- 요청 1회마다 `전체 온라인 세션 수 N`을 전부 스캔
- 실제로 필요한 값은 `내 친구 수 F`명에 대한 online 여부뿐

복잡도 관점:
- 현재: `O(N)` (`N = 전체 온라인 세션 수`)
- 이상적: `O(F)` (`F = 내 친구 수`)

즉, 트래픽이 늘수록 **친구 목록 조회가 전체 온라인 사용자 수에 비례해 비싸지는 구조**다.

### 1-2. DB 부하 관점

- 친구 기본 목록은 이미 캐시(`friend:list:%s:v1`)가 있어 DB 부하는 빈번하지 않다.
- 이번 변경의 핵심 경로는 DB가 아니라 Redis presence 조회다.
- 따라서 본 이슈의 주된 리스크는 DB 증폭이 아니라 **Redis 조회 패턴 비효율**이다.

### 1-3. 서버(CPU/메모리) 부하 관점

- `lobby:users` 전체 읽기 + set 생성은 요청마다 객체 할당이 커진다.
- 온라인 세션이 큰 시간대에는 GC/CPU 비용이 누적될 수 있다.
- 반대로 friendId 대상 배치 조회는 payload가 작아 API 서버 부담이 줄어든다.

---

## 2) 오버엔지니어링 여부 판단

결론: 기존 계획 일부는 과했다.

과한 부분:
- friend 전용 포트/구현체를 처음부터 크게 분리하는 것
- feature flag로 소스 전환까지 넣는 것
- 캐시 키 버전(`v2`) 마이그레이션을 선행하는 것

왜 과한가:
- 현재 요구사항은 `/friends`의 `online` 정확도 개선 1건
- 이미 연결 상태 데이터는 `WebSocketConnectionStateOrchestrator`가 관리 중
- 대규모 아키텍처 확장 없이도 목적 달성 가능

실무 권장:
- 1차는 최소 변경(MVP)으로 정확도/부하 둘 다 개선
- 운영 데이터 확인 후 필요할 때만 추상화/플래그 추가

---

## 3) 수정된 설계 원칙 (Lean)

- 온라인 기준: `websocket:connection:state:member:{id}`
- API 변경 최소화: 응답 스키마는 그대로(`FriendListResponse.online` 유지)
- 캐시 전면 개편 대신, 기존 캐시 데이터를 활용해 online만 동적 overwrite
- Redis 조회는 친구 ID 기준 배치 처리(파이프라인)

---

## 4) 수정된 구현 범위 (MVP 우선)

### 4-1. Presence 조회는 "서비스 1개 추가"로 끝낸다

추가 컴포넌트(최소):
- `FriendOnlineStatusService` (가칭)
  - 입력: `Collection<Long> friendMemberIds`
  - 출력: `Set<Long> onlineMemberIds`
  - 구현: Redis pipeline으로 각 member state key의 `state` 필드 조회

주의:
- 초기에는 friend 도메인 포트/어댑터 계층 분리하지 않는다.
- 필요 시 2차 리팩터링으로 추상화한다.

### 4-2. `GetMyFriendsUseCase` 교체

변경:
- `LobbyPresenceService` 의존 제거
- 친구 목록(캐시 hit/miss 모두)에 대해 friendMemberId 집합 추출
- `FriendOnlineStatusService` 배치 조회 결과로 `online` overwrite

효과:
- 캐시 hit여도 `online`은 최신 반영
- 로비 구독 누락 문제와 분리

### 4-3. 캐시는 당장 유지

이번 배포에서는:
- 캐시 구조/키 버전 변경 없음
- 기존 캐시 JSON 재활용 + 반환 직전 `online` 재계산

이유:
- 마이그레이션 리스크/개발량 축소
- 요구사항 달성에 충분

---

## 5) 온라인 판정 정책

1차 정책(단순/명확):
- `CONNECTED` -> true
- 그 외(`DISCONNECTED_TEMP`, `LEFT`, null) -> false

이유:
- API 의미가 직관적이고 운영 설명이 쉽다.
- `DISCONNECTED_TEMP=true`는 UX 상 장점이 있으나 해석 논쟁이 생긴다.

선택 옵션:
- 추후 필요 시 `DISCONNECTED_TEMP`를 true로 바꾸는 것은 작은 정책 변경으로 가능.

---

## 6) 상세 작업 순서 (수정)

### Step 1. 최소 구현
- `FriendOnlineStatusService` 추가
- Redis pipeline 조회 구현
- state -> online 매핑 유틸 추가

### Step 2. 유즈케이스 적용
- `GetMyFriendsUseCase`에서 로비 의존 제거
- 캐시 데이터 포함 전체 응답에 `online` overwrite

### Step 3. 테스트
- 단위: state 매핑, 빈 친구목록, 다건 friendId
- 통합: connect/disconnect 후 `/friends` online 반영
- 회귀: 기존 친구 목록 정상 응답 유지

### Step 4. 문서
- 친구 API 문서에 `online` 판정 기준 명시

---

## 7) 성능 검증 포인트

필수 확인:
- `/friends` 호출당 Redis ops가 `O(N)`에서 `O(F)`로 줄어드는지
- p95 응답시간이 악화되지 않는지
- 에러 시 안전하게 `online=false` 처리되는지

간단 지표:
- `friend_online_lookup_count`
- `friend_online_lookup_ms`
- `friend_online_lookup_error_count`

---

## 8) 리스크와 대응 (수정)

리스크 1: friend 수가 매우 큰 계정의 Redis 조회량 증가
- 대응: pipeline 사용, friend 수 상한/페이지네이션 정책 검토

리스크 2: connection state 키 누락
- 대응: 누락은 offline으로 처리, warn 로그 기록

리스크 3: Redis 일시 장애
- 대응: 친구 목록은 반환하되 `online=false` fallback

---

## 9) Acceptance Criteria (수정)

1. `/friends`의 `online`은 로비 참여 여부와 무관하게 WebSocket state 기준으로 계산된다.
2. 캐시 hit 시에도 `online`은 요청 시점 기준으로 재계산된다.
3. 친구 목록 조회의 presence 조회 비용이 전체 온라인 세션 수가 아니라 친구 수에 비례한다.
4. 장애/누락 상황에서 API 실패 대신 `online=false`로 안전 반환된다.

---

## 10) 일정 제안 (3일)

- Day 1: 서비스 구현 + 유즈케이스 반영
- Day 2: 테스트/문서 + 간단 성능 측정
- Day 3: 스테이징 검증 후 배포

---

## 11) 구현 체크리스트 (수정)

- [ ] `LobbyPresenceService` 기반 온라인 판정 제거
- [ ] `FriendOnlineStatusService` 추가 (Redis pipeline)
- [ ] 캐시 응답에 `online` 동적 overwrite 적용
- [ ] 단위/통합 테스트 추가
- [ ] 친구 API 문서에 온라인 기준 명시
- [ ] 배포 후 p95 및 Redis ops 확인

---

## 12) 2차 개선(선택)

아래는 1차 운영 데이터 확인 후 진행한다.
- friend 도메인 포트/어댑터 추상화
- `DISCONNECTED_TEMP` 정책 토글화
- 캐시 구조 정식 분리(정적/동적 필드 분리)
