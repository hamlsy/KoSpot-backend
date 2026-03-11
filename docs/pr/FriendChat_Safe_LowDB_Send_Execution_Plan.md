# Friend Chat 전송 경로 저DB/고안전 실행 계획

## 1) 목적과 성공 기준

### 목적
- 친구 채팅 메시지 전송(SEND) 경로에서 DB 접근을 최소화한다.
- 실시간 전달(Fan-out)과 영속화(Persist)를 분리해 장애 전파를 차단한다.
- 권한 검증의 보안 수준을 유지하면서도 TPS를 높인다.

### 성공 기준 (Done Definition)
- `SEND` 1건당 DB 조회/저장은 0회(정상 경로 기준).
- 권한 없는 사용자는 구독/전송 모두 차단된다.
- 메시지는 구독자에게 즉시 전달되고, DB는 비동기로 내구화된다.
- Redis Stream 장애 시 명확한 실패 응답을 반환하고, 부분 성공 상태가 발생하지 않는다.
- 멀티 인스턴스 환경에서도 권한/전송 동작이 동일하게 유지된다.

---

## 2) 현재 구조의 리스크 요약

대상 코드:
- `src/main/java/com/kospot/presentation/friend/controller/FriendWebSocketController.java`
- `src/main/java/com/kospot/application/friend/SendFriendChatMessageUseCase.java`

핵심 리스크:
- **실시간 브로드캐스트 부재/불명확**: STOMP 수신 후 room 구독자 전송 경로가 명시적으로 보장되지 않음.
- **SEND-DB 결합**: 메시지마다 채팅방 메타 갱신 저장이 일어나 write amplification 위험이 큼.
- **검증 비용 과다 가능성**: SEND마다 DB 참여자 검증을 수행하면 QPS 증가 시 DB 급증.
- **권한 검증 분산**: 구독 검증 체계와 친구채팅 검증이 일관되게 연결되어 있지 않음.

---

## 3) 실무 권장 아키텍처 (가장 안전한 방식)

핵심 원칙: **검증은 구독 시 1회(DB), 전송은 Redis O(1), 저장은 비동기 배치**

### 전체 흐름
1. 클라이언트 SUBSCRIBE: `/topic/friend/chat-rooms/{roomId}`
2. 서버는 SUBSCRIBE 시점에만 DB로 room 참여자 검증 1회 수행
3. 통과 시 `sessionId -> allowedRoomIds`를 Redis Set에 저장(TTL)
4. 클라이언트 SEND: `/app/chat-rooms.{roomId}.chat`
5. 서버는 SEND에서 Redis Set membership만 확인(`SISMEMBER`)
6. 통과 시 메시지 생성 후
   - Redis Stream enqueue (내구화 파이프라인 입력)
   - STOMP topic fan-out (실시간 전달)
7. Worker가 Stream 배치 처리
   - `friend_chat_message` 배치 insert
   - room별 `lastMessageAt` 배치 upsert/update

### 왜 안전한가
- 권한 검증을 "DB 고비용"에서 "Redis 저비용"으로 이동
- 실시간성과 내구화가 분리되어 DB 지연이 전송 지연으로 번지지 않음
- Redis Stream + DLQ로 실패 메시지 추적 가능

---

## 4) 세부 설계

## 4-1. 채널/목적지 표준화
- SEND: `/app/chat-rooms.{roomId}.chat`
- SUBSCRIBE: `/topic/friend/chat-rooms/{roomId}`
- 서버 내부 상수화 권장:
  - `FRIEND_CHAT_TOPIC_PREFIX = "/topic/friend/chat-rooms/"`
  - `FRIEND_CHAT_APP_PREFIX_PATTERN = "/chat-rooms.{roomId}.chat"`

목적:
- 파싱 오류/오타로 인한 권한 우회 및 운영 혼선 제거

## 4-2. SUBSCRIBE 시점 권한 검증 + 세션 권한 캐시

### 검증 정책
- 입력: `memberId(Principal)`, `roomId(destination)`
- DB 검증(1회): `room.isParticipant(memberId)`
- 통과 시 Redis 기록:
  - Key: `ws:friend:sub:{sessionId}`
  - Type: Set
  - Member: `{roomId}`
  - TTL: 30분 (heartbeat/재구독 시 연장)

### 인터셉터 연계
- `WebSocketChannelInterceptor.handleSubscribe`에서 친구채팅 prefix를 검출
- `SubscriptionValidationManager`에 `FriendChatSubscriptionValidator` 추가

### 이점
- SEND 시 DB 검증 제거 가능
- 멀티 서버에서도 동일한 권한 상태 공유

## 4-3. SEND 시점 경량 검증

### 검증 순서
1. Principal 확인
2. Destination에서 `roomId` 파싱
3. Redis `SISMEMBER ws:friend:sub:{sessionId} roomId` 확인
4. false면 즉시 403/웹소켓 에러 반환

### 성능
- Redis 단건 조회 1회
- DB 조회 0회

## 4-4. 메시지 처리 파이프라인

### Ingress (동기)
- `messageId`, `createdAt`, `senderMemberId(principal)`, `content` 생성/검증
- `FriendChatStreamProducer.enqueue(message)`
- enqueue 성공 시 `SimpMessagingTemplate.convertAndSend(topic, event)`

### Persist (비동기)
- 기존 `FriendChatPersistWorker` 활용
- 배치 insert 후 room별 최대 `createdAt` 기준으로 `friend_chat_room.last_message_at` 배치 업데이트

### 정합성 원칙
- **enqueue 실패 시 fan-out 금지** (수신자와 저장소 간 불일치 방지)
- **fan-out 실패 시 enqueue 성공 메시지는 재전송 불가**(현재 STOMP 특성), 대신 모니터링/재동기화 정책으로 보완

## 4-5. DTO/보안 정리
- 클라이언트가 보내는 `memberId`는 무시 또는 제거
- sender는 반드시 Principal 기반으로만 결정
- 메시지 길이 제한은 ingress에서 강제(예: 1~500)
- 빈 문자열/공백만 메시지 차단

## 4-6. 무효화(Revocation) 전략
- 친구 관계 해제/차단/채팅방 권한 변경 이벤트 발생 시:
  - 해당 member의 활성 세션 키에서 roomId 제거
  - 필요 시 강제 unsubscribe 또는 다음 SEND부터 차단
- TTL 기반 자연 만료 + 이벤트 기반 즉시 무효화 병행

---

## 5) Redis 키 설계

### 키 목록
- `ws:friend:sub:{sessionId}` (Set)
  - 값: roomId들
  - TTL: 1800s
- (선택) `friend:room:meta:{roomId}` (Hash)
  - 값: `memberLowId`, `memberHighId`, `updatedAt`
  - 용도: SUBSCRIBE DB miss 완화
- 기존 유지: friend chat stream 관련 키

### 운영 가이드
- session disconnect 시 `ws:friend:sub:{sessionId}` 즉시 삭제
- 키 개수/메모리 사용량 모니터링 필수

---

## 6) 장애/예외 시나리오와 처리

### 시나리오 A: Redis 장애
- 증상: SUBSCRIBE 캐시 저장 실패, SEND 검증 실패
- 정책: fail-closed (전송 차단) + 명확한 에러 코드 반환
- 이유: 권한 우회 방지

### 시나리오 B: Stream enqueue 실패
- 정책: fan-out 하지 않고 오류 반환
- 이유: 저장 파이프라인 입력 실패 상태에서 실시간 전송만 성공하면 데이터 불일치 발생

### 시나리오 C: Worker DB insert 실패
- 기존 DLQ/재시도 정책 활용
- max retry 초과 시 DLQ 적재 후 알람

### 시나리오 D: 구독 후 권한 상실
- revoke 이벤트로 Redis set에서 roomId 제거
- 이후 SEND 즉시 차단

---

## 7) 단계별 실행 계획 (P0/P1/P2)

## P0 - 즉시 안정화 (핫픽스)
목표: 현재 치명 리스크 제거

작업:
- `SendFriendChatMessageUseCase`의 room/principal 처리 버그 수정
- SEND 시 DB 저장(`saveRoom`) 제거 또는 비활성화
- friend chat fan-out 경로 명시 추가 (`SimpMessagingTemplate`)
- 메시지 sender를 principal 기반으로 고정

완료 조건:
- 메시지 송신 시 구독자가 실시간 수신
- 정상 경로에서 메시지당 DB write 0

## P1 - 저DB 권한 검증 체계 도입
목표: DB 검증을 SUBSCRIBE 1회로 제한

작업:
- `FriendChatSubscriptionValidator` 구현
- `WebSocketChannelInterceptor` 구독 검증 라우팅에 friend 채널 추가
- `ws:friend:sub:{sessionId}` Redis 캐시 저장/조회 로직 구현
- DISCONNECT/UNSUBSCRIBE 시 캐시 정리

완료 조건:
- SEND 경로 DB 조회 0
- 비인가 room 전송 100% 차단

## P2 - 내구화/운영 고도화
목표: 대규모 트래픽과 운영 안정성 강화

작업:
- worker에서 room `lastMessageAt` 배치 업데이트
- 모니터링 지표/알람 세팅
- 멀티 인스턴스 fan-out 전략 검토(브로커 릴레이 또는 pub/sub 연계)

완료 조건:
- p95/p99 send latency 개선
- DLQ율/worker lag가 SLO 내 유지

---

## 8) 테스트 전략

### 단위 테스트
- SUBSCRIBE 검증 성공/실패
- SEND Redis membership 성공/실패
- enqueue 실패 시 fan-out 미실행 검증
- sender spoofing 차단 검증

### 통합 테스트
- 실제 STOMP 연결 후
  - 참여자만 구독/전송 가능
  - 비참여자는 거부
  - 구독자 다중 수신 확인

### 부하 테스트
- 목표: 기존 대비 DB QPS 감소율 확인
- 관찰값:
  - DB QPS (friend chat 관련)
  - Redis ops/sec
  - send latency p95/p99
  - stream lag, DLQ count

---

## 9) 관측성/운영 지표

필수 메트릭:
- `friend_chat_send_total{result=success|denied|enqueue_fail}`
- `friend_chat_send_latency_ms` (p50/p95/p99)
- `friend_chat_sub_validation_total{result}`
- `friend_chat_stream_lag`
- `friend_chat_dlq_total`
- `redis_key_count{prefix=ws:friend:sub}`

알람 제안:
- enqueue_fail 비율 > 1% (5분)
- stream lag 임계치 초과
- denied 급증(권한 설정 오류 탐지)

---

## 10) 롤백 전략

- Feature flag 기반 단계적 적용
  - `friend.chat.send.redis-membership-enabled`
  - `friend.chat.send.direct-room-save-enabled` (기본 false)
- 문제 시
  1) Redis membership 검증 off
  2) 기존 DB 검증 fallback on
  3) fan-out만 유지 여부는 장애 성격에 따라 선택

주의:
- 롤백 경로도 권한 우회가 없도록 fail-closed 원칙 유지

---

## 11) 예상 효과

- SEND 트래픽이 높아져도 DB 병목이 급격히 증가하지 않음
- 실시간 전달 품질 개선(지연/스파이크 감소)
- 권한 검증 일관성 확보(구독/전송 모두 통제)
- 장애 분리(전송, 검증, 저장 파이프라인 분리)

---

## 12) 구현 체크리스트

- [ ] friend 채널 destination 상수 정의 및 통일
- [ ] FriendChatSubscriptionValidator 구현
- [ ] SUBSCRIBE 성공 시 Redis set 저장 + TTL 연장
- [ ] SEND 시 Redis membership 검증
- [ ] SendFriendChatMessageUseCase에서 principal 기반 sender 고정
- [ ] enqueue 성공 후 fan-out 수행
- [ ] 메시지 길이/공백 검증
- [ ] DISCONNECT/UNSUBSCRIBE 캐시 정리
- [ ] worker room 메타 배치 업데이트
- [ ] 메트릭/알람 추가
- [ ] 통합/부하 테스트 통과
