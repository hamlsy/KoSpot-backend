## Issue Title:

[Auth/WebSocket/GameFlow] 멀티탭 허용 + 게임 플로우 단일화 + 멀티디바이스 동시 로그인 차단

### Description

현재는 WebSocket 세션이 `session:ctx:{sessionId}` 단위로만 관리되어, 같은 계정이 여러 탭/여러 디바이스에서 동시에 접속해도 세션이 분기됩니다.

이번 이슈의 정책은 아래와 같습니다.

- **멀티탭 접속은 허용**
  - 같은 브라우저(동일 로그인 컨텍스트)에서 탭을 여러 개 열 수 있음
- **게임 플로우는 단일화**
  - 랭크/멀티 게임 진행 중 다른 탭으로 서비스에 진입하면, 새 게임 진입이 아니라 기존 진행 중 게임 상태로 합류/복원되어야 함
  - 결과적으로 "하나의 유저는 한 시점에 하나의 게임 플로우"만 가진다
- **멀티디바이스 동시 로그인은 차단(우선순위 높음)**
  - PC 로그인 중 모바일 로그인(또는 반대)이 발생하면 기존 디바이스 세션은 무효화
  - 기본 정책: **Last Login Wins**

현재 코드 상태(요약):
- `session:ctx:{sessionId}` 저장/TTL은 정상 (`SessionContextRedisService`)
- CONNECT/DISCONNECT 정리 경로는 존재 (`WebSocketChannelInterceptor`, `WebSocketConnectionStateOrchestrator`)
- 그러나 "계정 단위 디바이스 활성 세션 1개" 제어, "게임 플로우 단일화" 제어는 없음
- `SessionMappingService`는 존재하지만 실사용 경로가 없음

### Scope

- In scope
  - 멀티디바이스 동시 로그인 차단
  - 동일 계정의 랭크/멀티 게임 동시 플레이 차단
  - 멀티탭 진입 시 기존 게임 상태 복원/동기화
- Out of scope
  - 여러 디바이스를 허용하는 정책 전환(추후 별도 이슈)
  - 전면적인 인증 아키텍처 교체

### Task

- [ ] 정책/용어 확정
  - [ ] `디바이스 세션`(로그인 단위)과 `탭 세션`(WebSocket 연결 단위) 분리 정의
  - [ ] Last Login Wins 상세 규칙(무효화 시점, 사용자 안내 방식) 문서화

- [ ] Redis 키 모델 설계
  - [ ] `auth:active:member:{memberId}` -> 현재 활성 디바이스 세션 식별자
  - [ ] `auth:active:session:{deviceSessionId}` -> memberId/issuedAt 등 메타
  - [ ] `game:active:member:{memberId}` -> 현재 진행 중 게임 컨텍스트(mode, roomId/matchId, state)
  - [ ] 기존 `session:ctx:{sessionId}`는 탭 연결 정보로 유지하되, 상위 컨텍스트와 연결
  - [ ] TTL/정리 규칙(로그아웃, 강제 무효화, disconnect) 정의

- [ ] 인증 경로(HTTP) 단일 디바이스 적용
  - [ ] 로그인 성공 시 `memberId`의 기존 활성 디바이스 세션 무효화
  - [ ] refresh token 저장을 `토큰값 단건`이 아니라 `memberId(or jti) 인덱스`로 검증 가능하게 정리
  - [ ] `/auth/reIssue` 시 활성 디바이스 세션 불일치면 재발급 거부
  - [ ] `/auth/logout` 시 활성 세션 인덱스 + refresh token + 관련 컨텍스트 정리

- [ ] WebSocket 경로(탭) 가드 적용
  - [ ] CONNECT 시 현재 탭이 활성 디바이스 세션 소속인지 검증
  - [ ] 무효화된 디바이스의 탭은 SUBSCRIBE/SEND 차단 및 종료 유도
  - [ ] 구세션 disconnect가 최신 상태를 덮어쓰지 않도록 버전/토큰 검증

- [ ] 게임 플로우 단일화 적용(랭크/멀티)
  - [ ] 게임 시작/입장 시 `game:active:member:{memberId}` 선점 또는 기존 컨텍스트 반환
  - [ ] 다른 탭에서 진입 시 "신규 매치 생성"이 아니라 기존 게임 화면/상태 반환
  - [ ] 동일 계정의 동시 게임 시작 요청은 멱등 처리(기존 게임으로 리다이렉트)
  - [ ] 게임 종료/포기/강제퇴장 시 active game 컨텍스트 정리

- [ ] 에러 코드/클라이언트 계약 추가
  - [ ] `SESSION_REVOKED_BY_NEW_LOGIN` (다른 디바이스 로그인으로 세션 무효화)
  - [ ] `GAME_ALREADY_IN_PROGRESS` (이미 진행 중인 게임이 있어 기존 플로우로 진입)
  - [ ] 프론트 처리 가이드(복원 라우팅, 토스트/모달 문구) 정리

- [ ] 테스트 및 검증
  - [ ] 동일 브라우저 탭 2개: 한 탭에서 게임 중 다른 탭 진입 시 동일 게임으로 복원
  - [ ] 동일 계정 PC+모바일: 후행 로그인 시 선행 디바이스 세션 즉시 무효화
  - [ ] 무효화된 refresh token/reIssue 실패 검증
  - [ ] 동시 요청(로그인/CONNECT/게임입장) 경합 테스트
  - [ ] Redis orphan key 누수 및 정리 검증

### Acceptance Criteria

- 멀티탭은 가능하되, 한 계정은 한 시점에 하나의 랭크/멀티 게임 플로우만 가진다.
- 게임 진행 중 다른 탭으로 접속하면 새 게임 생성 없이 기존 게임 상태로 진입한다.
- 동일 계정 다중 디바이스 동시 로그인은 불가하며, 최신 로그인만 유효하다.
- 무효화된 디바이스 세션의 WebSocket/토큰 재사용은 거부된다.
- 관련 Redis 인덱스(`auth:active:*`, `game:active:*`, `session:ctx:*`)가 정책에 맞게 일관되게 정리된다.

### Comment

- 본 이슈는 구현 전 정책 확정 및 구현 범위 고정을 위한 기준 문서다.
- 구현 시에는 로그인/접속/게임입장 경합을 고려해 원자적 갱신(Lua 또는 트랜잭션)을 사용한다.
