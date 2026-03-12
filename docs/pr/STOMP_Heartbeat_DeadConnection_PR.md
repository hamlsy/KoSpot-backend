# PR: STOMP Heartbeat 적용 및 비정상 연결 상태 오케스트레이션 도입

## 배경

멀티플레이 WebSocket 통신에서 STOMP heartbeat가 미설정 상태여서 dead connection 감지가 지연될 수 있었고, disconnect 처리도 이벤트 소스별로 분산되어 중복/경합 위험이 있었다. 이번 PR은 heartbeat 기반 생존성 체크를 서버에 적용하고, disconnect 이후 상태 전환/최종 정리를 단일 오케스트레이션 경로로 통합한다.

## 이번 PR 범위

- Day 2: 서버 STOMP broker heartbeat 활성화 및 설정 외부화
- 우선순위 1: disconnect 처리 단일화(`DISCONNECTED_TEMP -> grace -> LEFT`) 도입
- 재접속 시 stale finalize 무효화(`disconnectToken`) 처리

복구(게임 문맥 복원) 고도화는 후속 작업 범위로 분리한다.

---

## 변경 사항

## 1) STOMP heartbeat 설정 추가

대상 파일:
- `src/main/java/com/kospot/common/websocket/config/WebSocketConfig.java`
- `src/main/java/com/kospot/common/websocket/config/WebSocketHeartbeatProperties.java`
- `src/main/resources/application.yml`

핵심 변경:
- SimpleBroker에 heartbeat 적용
  - `setHeartbeatValue([serverOutgoingMs, serverIncomingMs])`
  - `setTaskScheduler(webSocketHeartbeatTaskScheduler)`
- heartbeat 관련 설정값 외부화
  - `websocket.heartbeat.server-outgoing-ms`
  - `websocket.heartbeat.server-incoming-ms`
  - `websocket.heartbeat.dead-threshold-ms`
  - `websocket.heartbeat.grace-period-ms`
  - `websocket.heartbeat.scheduler-pool-size`

## 2) 연결 상태머신 도입

대상 파일:
- `src/main/java/com/kospot/common/websocket/connection/domain/WebSocketConnectionState.java`

상태:
- `CONNECTED`
- `DISCONNECTED_TEMP`
- `RECONNECTED`
- `LEFT`

## 3) disconnect 오케스트레이션 단일화

대상 파일:
- `src/main/java/com/kospot/common/websocket/connection/service/WebSocketConnectionStateOrchestrator.java`
- `src/main/java/com/kospot/common/websocket/handler/WebSocketEventHandler.java`
- `src/main/java/com/kospot/common/websocket/interceptor/WebSocketChannelInterceptor.java`

핵심 흐름:
1. disconnect 이벤트 수신 시 세션/구독 캐시 정리
2. 멤버 상태를 `DISCONNECTED_TEMP`로 전환
3. grace 만료 시 finalize 작업 스케줄링
4. grace 내 reconnect 발생 시 `disconnectToken` 제거 + `RECONNECTED -> CONNECTED`
5. grace 만료 시점에도 여전히 `DISCONNECTED_TEMP`이면 `LEFT` 확정 및 room leave 수행

설계 포인트:
- 즉시 탈락이 아닌 임시 이탈 기반 처리
- 토큰 검증 기반 stale finalize 방지
- disconnect 처리 로직의 단일 진입점 보장

---

## 왜 이 변경이 핵심인가

- heartbeat는 감지 신호일 뿐이고, 실제 안정성은 감지 이후 처리 일관성에서 결정된다.
- 기존처럼 disconnect 처리 경로가 분산되면 중복 leave, 정리 경합, 상태 꼬임이 발생하기 쉽다.
- 이번 PR은 disconnect/재접속/최종 이탈을 같은 상태머신으로 수렴시켜 dead connection 대응의 기준 경로를 만든다.

---

## 동작 시나리오

### 비정상 종료
- 연결 끊김 감지 -> `DISCONNECTED_TEMP`
- grace 내 복구 실패 -> `LEFT` + 방 나감 처리

### 일시적 네트워크 단절
- 연결 끊김 감지 -> `DISCONNECTED_TEMP`
- grace 내 reconnect 성공 -> `RECONNECTED -> CONNECTED`
- 예약된 최종 이탈 작업은 token/state 불일치로 no-op

---

## 영향도

- WebSocket disconnect 처리 경로가 변경됨
- 방 퇴장 타이밍이 "즉시"에서 "grace 기반 최종 확정"으로 변경됨
- 운영 중에는 grace/heartbeat 파라미터 튜닝 필요

---

## 테스트/검증

진행:
- 코드 레벨 리뷰 및 이벤트 플로우 검증

제약:
- 로컬 `./gradlew compileJava` 실행 시 환경 이슈로 실패
  - `Could not find or load main class worker.org.gradle.process.internal.worker.GradleWorkerMain`

권장 검증:
- stage에서 네트워크 off/on, 앱 강종, reconnect 시나리오 검증
- timeout/reconnect/cleanup 메트릭 관측

---

## 커밋

- `cab2e7f1` feat: enable configurable STOMP broker heartbeat
- `ed98c57b` feat: add websocket disconnect state orchestration

---

## 후속 작업 (Out of Scope)

- heartbeat timeout 이벤트를 오케스트레이터 진입점에 명시적으로 연결
- reconnect 시 게임 문맥 복구(턴/진행상태) 고도화
- 메트릭/알람 대시보드 및 운영 runbook 완성
