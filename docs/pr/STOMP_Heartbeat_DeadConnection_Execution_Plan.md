# STOMP Heartbeat 적용으로 Dead Connection 감지/세션 정리 개선 실행 계획

## 0) 배경과 목표

현재 멀티플레이 WebSocket 통신은 STOMP heartbeat 정책이 명시적으로 설정되어 있지 않아, 네트워크 단절/앱 비정상 종료 상황에서 dead connection 감지와 세션 정리가 지연될 수 있다. 이 문서는 heartbeat 기반 생존성 확인을 도입하고, 감지 이후 상태 전환/정리/재접속 복구까지 일관되게 연결하기 위한 실무 실행 계획이다.

핵심 목표:
- dead connection 감지 시간을 기존 대비 단축
- 감지 후 `session -> player -> room/game` 상태 정합성 유지
- 즉시 탈락 대신 `DISCONNECTED_TEMP + grace period`로 재접속 UX 보장
- 로그/메트릭 기반 운영 가시성 확보

---

## 1) 현재 코드 기준 진단 포인트

코드 확인 대상:
- `src/main/java/com/kospot/common/websocket/config/WebSocketConfig.java`
- `src/main/java/com/kospot/common/websocket/interceptor/WebSocketChannelInterceptor.java`
- `src/main/java/com/kospot/common/websocket/handler/WebSocketEventHandler.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`
- `src/main/resources/application-test.yml`

확인된 사실:
- `WebSocketConfig`에서 `enableSimpleBroker("/topic", "/queue")`는 설정되어 있으나, heartbeat 값(`setHeartbeatValue`) 및 scheduler(`setTaskScheduler`) 설정이 없다.
- STOMP 연결/구독/전송 인터셉트 및 disconnect 이벤트 처리(`SessionDisconnectEvent`)는 이미 존재한다.
- `WebSocketEventHandler.handleWebSocketDisconnectListener`는 disconnect 시점에 lobby/room leave 및 세션 정리를 수행한다.
- 현재는 "disconnect 이벤트가 언제 올라오느냐"에 의존하는 구조라, 물리적 단절이 즉시 감지되지 않는 구간에서 정리 지연이 발생할 수 있다.

시사점:
- heartbeat 도입 자체보다 중요한 것은, heartbeat timeout을 기존 disconnect 정리 흐름과 충돌 없이 결합하는 것이다.

---

## 2) 설계 원칙

- heartbeat는 "연결 생존성"만 판정하며, 화면 상태(Room/InGame/Result)와 분리 관리한다.
- dead connection 처리 경로를 단일화한다.
  - 입력 이벤트: `SessionDisconnectEvent`, heartbeat timeout, 강제 종료
  - 내부 처리: 동일한 상태 전환/정리 UseCase 진입
- 모든 전환은 멱등(idempotent)해야 한다.
  - 중복 disconnect/timeout 이벤트가 와도 최종 상태와 side effect는 1회만 반영
- 즉시 탈락 대신 임시 이탈 상태를 도입한다.
  - `CONNECTED -> DISCONNECTED_TEMP -> RECONNECTED | LEFT`

---

## 3) 목표 상태머신

```text
CONNECTED
  └─(heartbeat timeout or transport disconnect)→ DISCONNECTED_TEMP [grace timer start]

DISCONNECTED_TEMP
  ├─(reconnect + session restore success within grace)→ RECONNECTED → CONNECTED
  └─(grace expired)→ LEFT [final room/game cleanup]
```

전이 규칙:
- `DISCONNECTED_TEMP` 진입은 1회성으로 처리한다.
- `LEFT` 확정 전에는 "임시 이탈" 규칙을 적용한다(즉시 패배/강퇴 미확정).
- grace 내 복구 성공 시 기존 게임 컨텍스트를 복원하고 이어서 진행한다.

---

## 4) Heartbeat 정책 제안 (초기값)

모바일 환경과 서버 부하를 함께 고려한 초기 권장값:

| 항목 | 권장값 | 비고 |
|---|---:|---|
| Client Outgoing Heartbeat | 10000ms | 클라이언트 -> 서버 |
| Client Incoming Heartbeat | 10000ms | 서버 heartbeat 수신 기대 |
| Server Outgoing Heartbeat | 10000ms | 서버 -> 클라이언트 |
| Server Incoming Heartbeat | 10000ms | 클라이언트 heartbeat 수신 기대 |
| Dead 판정 임계 | 30000ms | interval의 약 3배(지터 허용) |
| Reconnect Grace Period | 45000~90000ms | 게임 모드별 차등 가능 |

운영 원칙:
- timeout은 1회 누락이 아니라 누적 무수신 시간으로 판단한다.
- 모바일 background/일시적 망 흔들림을 고려해 false positive를 줄인다.
- 초기 배포 후 timeout rate/reconnect 성공률을 보고 interval과 grace를 조정한다.

---

## 5) 구현 범위 및 변경 포인트

## 5-1. 서버(STOMP Broker) heartbeat 활성화

대상: `WebSocketConfig`

작업:
- `MessageBrokerRegistry.enableSimpleBroker(...)` 반환값(`SimpleBrokerRegistration`)에 heartbeat 적용
- heartbeat scheduler(`TaskScheduler`) 등록
- 설정값 하드코딩 금지, `application*.yml` 기반 외부화

예시 형태:

```java
config.enableSimpleBroker("/topic", "/queue")
      .setHeartbeatValue(new long[]{serverOutgoingMs, serverIncomingMs})
      .setTaskScheduler(heartbeatTaskScheduler);
```

주의:
- scheduler thread 부족 시 heartbeat 지연/오탐 가능성 있으므로 풀 사이즈를 명시 관리
- 기존 inbound/outbound executor와 역할이 다르므로 분리 유지

## 5-2. 설정 프로퍼티 확장

대상:
- `application.yml`
- `application-local.yml`
- `application-test.yml`

권장 키:

```yaml
websocket:
  allowed-origins: ...
  heartbeat:
    server-outgoing-ms: 10000
    server-incoming-ms: 10000
    dead-threshold-ms: 30000
    grace-period-ms: 60000
```

추가 권장:
- `@ConfigurationProperties` 전용 클래스(`WebSocketHeartbeatProperties`) 생성
- 환경별(dev/stage/prod) override 전략 문서화

## 5-3. disconnect/timeout 처리 오케스트레이션 통합

현재 `WebSocketEventHandler`에 분산된 disconnect 후처리를 다음 형태로 통합한다.

작업:
- `ConnectionStateOrchestrator`(가칭) 또는 전용 UseCase 도입
- 입력: `memberId`, `sessionId`, `reason`, `occurredAt`
- 동작:
  - 임시 이탈 전환(`DISCONNECTED_TEMP`)
  - room/game 반영(임시 이탈 규칙)
  - grace 타이머 등록
  - 최종 확정 시 `LEFT` 처리

원칙:
- `SessionDisconnectEvent`와 heartbeat timeout이 같은 오케스트레이터를 호출
- 이미 처리된 세션이면 no-op

## 5-4. 세션 컨텍스트/정리 정책

기존 사용 컴포넌트:
- `SessionContextRedisService`
- `WebSocketSessionService`

보강 포인트:
- 세션 상태 키에 `connectionState`, `lastHeartbeatAt`, `graceExpireAt` 저장
- 정리 순서 표준화:
  1) 상태 전환
  2) room/game 반영
  3) subscription/session cache 제거
  4) 세션 컨텍스트 삭제

## 5-5. 재접속 연계

재접속 성공 기준:
- 동일 member의 새 STOMP 세션 인증 성공
- grace 미만
- room/game 복구 조건 충족

실패 기준:
- grace 만료 후 reconnect
- room/game 컨텍스트 불일치

처리:
- 성공 시 `RECONNECTED -> CONNECTED`
- 실패 시 `LEFT` 유지 + 신규 입장 플로우 유도

---

## 6) 클라이언트 적용 가이드 (연동 계약)

- STOMP client heartbeat 활성화
  - `heartbeatOutgoing = 10000`
  - `heartbeatIncoming = 10000`
- 앱 lifecycle 이벤트 대응
  - background 진입 시 즉시 disconnect 강제는 피하고, 네트워크 상태 변화와 함께 재시도 정책 유지
- reconnect backoff
  - 짧은 지수 백오프 + 상한값 설정(과도한 재연결 방지)
- 서버와의 계약
  - 임시 이탈 상태 수신 시 UI는 "재연결 시도 중" 표시
  - 최종 이탈 확정 이벤트 수신 시 룸/게임 화면 전환

---

## 7) 로깅/메트릭/알람 설계

## 7-1. 구조화 로그

필수 필드:
- `memberId`, `sessionId`, `roomId`, `stateBefore`, `stateAfter`, `reason`, `elapsedMs`, `graceMs`

필수 로그 이벤트:
- heartbeat timeout 감지
- `CONNECTED -> DISCONNECTED_TEMP`
- reconnect 성공(`DISCONNECTED_TEMP -> RECONNECTED`)
- grace 만료(`DISCONNECTED_TEMP -> LEFT`)

## 7-2. 메트릭

권장 지표:
- `ws_heartbeat_timeout_total`
- `ws_reconnect_success_total`
- `ws_reconnect_fail_total`
- `ws_disconnect_to_cleanup_ms` (histogram)
- `ws_disconnected_temp_gauge`

## 7-3. 알람

초기 기준(예시):
- timeout rate 급증(5분 이동평균 기준 임계 초과)
- reconnect success ratio 급락
- cleanup latency p95 악화

---

## 8) 테스트 전략

## 8-1. 단위 테스트

- 상태머신 전이 테스트
  - 중복 timeout 이벤트 멱등성
  - grace 내/외 reconnect 분기
- 오케스트레이터 테스트
  - 같은 세션에 대한 disconnect+timeout 경합 처리

## 8-2. 통합 테스트

- STOMP heartbeat 송수신 검증
- heartbeat 미수신 시 timeout 판정 및 상태 전환 검증
- room/game/session 정리 부작용 검증

## 8-3. 시나리오 테스트(수동/E2E)

- foreground <-> background 전환
- 네트워크 off/on, Wi-Fi <-> LTE 전환
- 앱 강제 종료(OS kill 포함)
- 서버 재시작/일시 부하 상황

합격 기준:
- dead 감지 평균 시간이 기존 대비 단축
- room 인원/게임 참여 상태 불일치 없음
- grace 내 재접속 시 게임 복구 성공

---

## 9) 배포 및 롤아웃 전략

- 1단계: stage에 heartbeat만 활성화, 관측 지표 확보
- 2단계: `DISCONNECTED_TEMP + grace` 로직 활성화(feature flag)
- 3단계: 트래픽 일부 카나리 적용 후 전체 확장

운영 안전장치:
- heartbeat/grace를 설정값으로 즉시 조정 가능해야 함
- timeout 급증 시 즉시 완화(임계 상향/feature flag off)

---

## 10) 작업 분해 (Jira 변환용)

Epic: STOMP Heartbeat 기반 연결 생존성/세션 정합성 개선

Story A. 서버 heartbeat 활성화
- `WebSocketConfig`에 heartbeat + scheduler 적용
- 프로퍼티 외부화 및 환경별 기본값 반영

Story B. 연결 상태머신 도입
- `DISCONNECTED_TEMP`, `RECONNECTED`, `LEFT` 전이 구현
- 오케스트레이터/UseCase 통합 경로 구현

Story C. 재접속 연계
- grace 기간 내 복구 조건 구현
- 복구 성공/실패 이벤트 및 후처리

Story D. 관측성
- 구조화 로그 + 메트릭 + 알람 룰 추가

Story E. 검증
- 단위/통합/E2E 시나리오 테스트 및 결과 리포트

---

## 11) Acceptance Criteria 매핑

- 클라이언트/서버 heartbeat 송수신 정상
  - STOMP frame/log/메트릭으로 확인 가능
- 네트워크 단절/앱 비정상 종료 시 기존 대비 빠른 dead 감지
  - 감지 지연 시간 지표로 전후 비교
- dead 감지 후 플레이어 상태 적절 전환
  - `CONNECTED -> DISCONNECTED_TEMP -> RECONNECTED/LEFT` 추적 가능
- 방 인원/게임 상태 정합성 유지
  - 운영 중 불일치 케이스 0건 목표
- 재접속 가능 시 기존 복구 로직과 연계
  - grace 내 reconnect 성공률/복구시간 측정

---

## 12) 리스크 및 대응

- heartbeat interval 과도 단축
  - 리스크: 모바일 배터리/네트워크 비용 증가
  - 대응: 10s 시작, 데이터 기반 점진 조정
- 모바일 background 제약으로 오탐
  - 대응: dead 임계 3배, 즉시 탈락 금지, grace 적용
- 중복 이벤트로 인한 상태 꼬임
  - 대응: 세션 단위 멱등 키 및 원자적 상태 전환
- reconnect/cleanup 경합
  - 대응: 단일 오케스트레이터 + 락/원자 연산으로 직렬화

---

## 13) 최종 권고

- 이번 이슈는 "heartbeat 설정"만으로 닫지 말고, 반드시 상태머신/정리/재접속까지 하나의 배포 단위로 묶어야 한다.
- 1차 릴리스는 보수적으로 `10s heartbeat + 30s dead threshold + 60s grace`를 권장한다.
- 릴리스 후 1~2주 관측 데이터를 기준으로 게임 모드별 grace 세분화를 진행한다.

---

## 14) 세부 실행 계획 (2주 기준)

아래 계획은 백엔드 1명 + 클라이언트 1명 + QA 1명 기준이며, 병렬 가능한 작업을 반영했다.

## Week 1: 설계 고정 + 서버 핵심 구현

### Day 1 - 현황 고정 및 정책 확정
- 현재 disconnect 처리 시퀀스 다이어그램 작성
- 상태머신 전이 규칙(`CONNECTED/DISCONNECTED_TEMP/RECONNECTED/LEFT`) 최종 확정
- heartbeat/grace 기본값 확정 (`10s/30s/60s`)
- 산출물: 설계 결정서(ADR) 1건, 전이표 1건

### Day 2 - WebSocketConfig heartbeat 적용
- `WebSocketConfig`에 broker heartbeat/scheduler 적용
- scheduler bean 분리(heartbeat 전용)
- 설정값 외부화(`application*.yml` + properties class)
- 산출물: 서버 heartbeat 활성화 PR

### Day 3 - ConnectionState 오케스트레이터 골격
- `ConnectionStateOrchestrator`(가칭) 생성
- 입력 커맨드 모델 정의(`memberId/sessionId/reason/occurredAt`)
- 멱등 키 전략 정의(`memberId+sessionVersion` 또는 `sessionId` 기반)
- 산출물: 오케스트레이터 기본 흐름 및 인터페이스

### Day 4 - DISCONNECTED_TEMP 전환/그레이스 등록
- timeout/disconnect 공통 진입점에서 `DISCONNECTED_TEMP` 전환
- grace 만료 예약 작업(스케줄/지연 큐) 연결
- 중복 전환 no-op 처리
- 산출물: 임시 이탈 + grace 타이머 동작

### Day 5 - LEFT 확정 및 room/game 연계
- grace 만료 시 최종 `LEFT` 확정 처리
- 기존 leave 로직과 충돌 방지(중복 side effect 제거)
- room 인원/세션 정리 순서 고정
- 산출물: 최종 이탈 처리까지 end-to-end 연결

## Week 2: 재접속/관측성/테스트/롤아웃

### Day 6 - 재접속 복구 경로 구현
- grace 내 reconnect 성공 시 `RECONNECTED -> CONNECTED`
- 기존 room/game 컨텍스트 복구 검증
- 복구 실패 분기(신규 입장 유도) 구현
- 산출물: reconnect 성공/실패 분기 완성

### Day 7 - 클라이언트 heartbeat/reconnect 정책 적용
- STOMP client heartbeat out/in 설정
- 앱 lifecycle 대응(background/foreground)
- 재연결 backoff 및 서버 상태 이벤트 반영
- 산출물: 클라이언트 연동 PR

### Day 8 - 로그/메트릭/알람 추가
- 구조화 로그 필드 표준 적용
- timeout/reconnect/cleanup 메트릭 추가
- 운영 알람 임계치 초안 설정
- 산출물: 대시보드/알람 룰 초안

### Day 9 - 통합 및 장애 시나리오 테스트
- 네트워크 off/on, 앱 강종, 서버 재기동 시나리오 수행
- 감지시간/복구성공률/정합성 측정
- 이슈 재현 케이스 회귀 확인
- 산출물: 테스트 리포트 v1

### Day 10 - 카나리 배포 및 운영 인계
- feature flag 기반 단계 배포
- 24시간 모니터링 + 파라미터 튜닝
- 장애 대응 runbook 배포/공유
- 산출물: 운영 배포 완료 및 릴리스 노트

---

## 15) 세부 태스크 분해 (WBS)

## 15-1. 서버 개발 WBS

1. Heartbeat 설정
- `WebSocketConfig`에 `setHeartbeatValue`, `setTaskScheduler` 적용
- heartbeat scheduler bean 생성 및 thread pool sizing
- DoD: 서버 로그에서 heartbeat 송신 확인, smoke test 통과

2. 설정 프로퍼티
- `websocket.heartbeat.*` 키 추가
- `@ConfigurationProperties` 바인딩 + validation
- DoD: local/test/prod profile에서 override 확인

3. 상태머신 도입
- enum/state model 추가
- 전이 함수(허용/비허용 전이) 구현
- DoD: 전이 단위 테스트 100% 통과

4. 오케스트레이터
- disconnect/timeout 단일 진입점 구현
- 멱등 처리 + 동시성 보호(락/원자 처리)
- DoD: 중복 이벤트 주입 테스트에서 side effect 1회 보장

5. grace 만료 처리
- 지연 실행 등록/취소 로직 구현
- 만료 시 `LEFT` 최종 처리 연계
- DoD: grace 내 reconnect 시 만료 작업 취소 검증

6. 재접속 복구
- 새 세션 귀속 검증 + 상태 복원
- 복구 성공/실패 이벤트 발행
- DoD: reconnect 성공 시 게임 지속, 실패 시 신규 진입 처리

7. 정리 로직 통합
- session/subscription/cache/redis attr 정리 순서 통일
- 기존 `LeaveGameRoomUseCase`와 충돌 제거
- DoD: 정리 후 유령 세션/중복 leave 없음

## 15-2. 클라이언트 개발 WBS

1. STOMP heartbeat 설정
- outgoing/incoming 10s 적용
- 연결 옵션 환경값 주입 가능화
- DoD: 정상 연결 시 heartbeat frame 송수신 확인

2. reconnect 정책
- 지수 백오프 + 최대 지연 상한 설정
- 네트워크 복구 시 즉시 재시도 트리거
- DoD: 일시적 단절에서 자동 복구 성공

3. UI 상태 반영
- `DISCONNECTED_TEMP` 수신 시 재연결 안내
- `LEFT` 수신 시 화면 전환/안내
- DoD: UX 플로우 리뷰 통과

## 15-3. QA WBS

1. 테스트 케이스 설계
- 정상/비정상/경계 케이스 분류
- 모바일 lifecycle 케이스 포함
- DoD: 케이스 목록 승인

2. 통합 검증
- dead 감지 시간 전후 비교
- room/game 상태 정합성 검증
- DoD: AC 전 항목 pass

3. 회귀 테스트
- 기존 채팅/구독/알림 영향도 확인
- DoD: P1 버그 0건

---

## 16) 작업 우선순위 및 의존성

- P0 (필수 선행)
  - 서버 heartbeat 설정
  - 상태머신 + 오케스트레이터
  - grace 만료/재접속 핵심 분기
- P1 (운영 품질)
  - 구조화 로그/메트릭/알람
  - 클라이언트 UX 고도화
- P2 (최적화)
  - 모드별 동적 grace
  - 임계치 자동 튜닝 룰

의존성:
- 클라이언트 heartbeat 적용은 서버 heartbeat 활성화 이후 검증 효율이 높다.
- 카나리 배포는 최소 로그/메트릭이 준비되어야 진행 가능하다.

---

## 17) 상세 DoD (Definition of Done)

기능 DoD:
- heartbeat 미수신 시 30초 내 dead 판정
- 판정 후 `DISCONNECTED_TEMP` 전환 이벤트 1회만 발생
- grace 내 reconnect 시 게임 상태 복원 성공
- grace 만료 시 `LEFT` 확정 및 정리 완료

품질 DoD:
- 단위/통합/E2E 테스트 통과
- 운영 로그/메트릭 대시보드 확인 가능
- 카나리 24시간 동안 치명 장애 없음

문서 DoD:
- 운영 runbook(장애 대응/롤백 절차) 배포
- 환경별 설정값 표 최신화

---

## 18) 운영 Runbook 요약

장애 징후:
- timeout rate 급증
- reconnect success 급락
- cleanup latency p95 급등

즉시 대응 순서:
1. heartbeat dead threshold 임시 상향 (`30s -> 45s`)
2. grace period 확장 (`60s -> 90s`)
3. 필요 시 feature flag로 `DISCONNECTED_TEMP` 로직 비활성화
4. 근본 원인 분석 후 재활성화

롤백 기준:
- reconnect 실패율이 기준치 초과(예: 5분 평균 20% 이상)
- room/game 정합성 오류가 연속 발생
