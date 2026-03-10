# 멀티 결과창 상대 화면 상태 실시간 동기화 실행 계획 (코드 재검증 반영본)

## 0) 코드 재검증 결과 (중요)

대상 확인 파일:
- `src/main/java/com/kospot/multi/room/infrastructure/redis/dao/GameRoomRedisRepository.java`
- `src/main/java/com/kospot/multi/room/application/handler/GameRoomEventHandler.java`
- `src/main/java/com/kospot/multi/room/domain/vo/GameRoomPlayerInfo.java`
- 연계 확인: `GameRoomRedisService`, `GameRoomNotification`, `GameRoomNotificationType`

핵심 사실:
- 현재 room player는 Redis Hash(`game:room:{roomId}:players`)에 **memberId -> playerJson 문자열**로 저장된다.
- `GameRoomRedisRepository`는 단순 `HGET/HSET/HDEL` 수준이며, **조건부 갱신(CAS) 기능이 없다**.
- `GameRoomEventHandler.handleJoin`은 Redis에 저장된 객체를 재사용하지 않고, 별도 빌드(`joinedAt` 재생성) 후 알림한다.
- `GameRoomNotificationType`/`GameRoomNotification`에 screen-state delta 타입이 아직 없다.

=> 결론: "playerList 통합" 방향은 맞지만, **seq 기반 정합성은 원자적 갱신 로직을 추가**해야 안전하다.

---

## 1) 최종 구현 방향
- 상태 채널을 신규 분리하지 않고, 기존 `/topic/room/{roomId}/playerList`에 통합한다.
- `GameRoomPlayerInfo`에 화면 상태 필드를 추가한다.
- 상태 변경은 `SCREEN_STATE_UPDATED` delta 이벤트로 즉시 브로드캐스트한다.
- 기존 `PLAYER_LIST_UPDATED`는 정합성 복구용으로 유지한다.

---

## 2) 도메인 모델 변경

## 2-1. 화면 상태 Enum
```java
public enum MultiplayerScreenState {
    IN_GAME,
    RESULT,
    ROOM,
    DISCONNECTED
}
```

권장:
- P0는 `IN_GAME`, `RESULT`, `ROOM` 우선 적용
- `DISCONNECTED`는 현재 disconnect 정책(즉시 leave 처리)과 충돌 가능성 있으므로 P1에서 재검토

## 2-2. `GameRoomPlayerInfo` 필드 추가
- `screenState: MultiplayerScreenState`
- `screenStateSeq: Long`
- `screenStateUpdatedAt: Long`

초기값:
- 방 생성/입장 시 `ROOM`, `screenStateSeq=0`, `screenStateUpdatedAt=now`

---

## 3) WebSocket/STOMP 스펙

### 3-1. Client -> Server
- `/app/room.{roomId}.screen.state`

요청:
```json
{
  "state": "RESULT",
  "clientSeq": 12,
  "clientTimestamp": 1760000000000
}
```

### 3-2. Server -> Client
- 채널: `/topic/room/{roomId}/playerList` (기존 재사용)
- 타입:
  - `SCREEN_STATE_UPDATED` (delta)
  - `PLAYER_LIST_UPDATED` (full sync)

delta 예시:
```json
{
  "type": "SCREEN_STATE_UPDATED",
  "roomId": "123",
  "playerInfo": {
    "memberId": 456,
    "screenState": "ROOM",
    "screenStateSeq": 13,
    "screenStateUpdatedAt": 1760000000456
  },
  "timestamp": 1760000000456
}
```

---

## 4) Redis 정합성 설계 (현재 저장구조 맞춤)

## 4-1. 왜 추가 설계가 필요한가
- 현재 저장값이 JSON 문자열이라, 단순 read-modify-write는 멀티 인스턴스에서 역전 가능성이 높다.

## 4-2. 권장 구현
- `GameRoomRedisRepository`에 Lua 기반 원자 갱신 메서드 추가:
  - 입력: `roomKey`, `memberId`, `newState`, `newSeq`, `updatedAt`
  - 동작: `HGET -> seq 비교 -> 조건 충족 시 HSET`
  - 반환: `UPDATED | STALE | NOT_FOUND`

## 4-3. 규칙
- `newSeq < currentSeq` => `STALE` (drop)
- `newSeq == currentSeq` => no-op (멱등)
- `newSeq > currentSeq` => 반영 + `screenStateUpdatedAt` 갱신

---

## 5) 서버 코드 수정 포인트 (파일 단위)

## 5-1. VO/Enum
- `src/main/java/com/kospot/multi/room/domain/vo/GameRoomPlayerInfo.java`
  - 화면 상태 3필드 추가
- (신규) `src/main/java/com/kospot/multi/room/domain/vo/MultiplayerScreenState.java`

## 5-2. Redis
- `src/main/java/com/kospot/multi/room/infrastructure/redis/dao/GameRoomRedisRepository.java`
  - Lua 원자 갱신 메서드 추가
- `src/main/java/com/kospot/multi/room/infrastructure/redis/service/GameRoomRedisService.java`
  - `updatePlayerScreenStateIfNewer(roomId, memberId, state, seq, updatedAt)` 추가
  - 성공 시 최신 `GameRoomPlayerInfo` 반환

## 5-3. Notification
- `src/main/java/com/kospot/multi/room/domain/vo/GameRoomNotificationType.java`
  - `SCREEN_STATE_UPDATED` 추가
- `src/main/java/com/kospot/multi/room/domain/vo/GameRoomNotification.java`
  - `screenStateUpdated(...)` 팩토리 추가
- `src/main/java/com/kospot/multi/room/infrastructure/websocket/service/GameRoomNotificationService.java`
  - `notifyPlayerScreenStateUpdated(roomId, playerInfo)` 추가

## 5-4. UseCase/Controller
- (신규) `UpdatePlayerScreenStateUseCase`
  - principal 기반 memberId 추출
  - room 참여자 검증
  - redis 원자 갱신
  - 성공 시 delta 브로드캐스트
- Controller 추가 위치(택1, 중복 금지):
  - `GameRoomWebSocketController` 또는 `MultiGameWebSocketController`
  - `@MessageMapping("/room.{roomId}.screen.state")`

## 5-5. Interceptor/EventHandler
- `WebSocketChannelInterceptor.handleSend`
  - 상태 이벤트 destination을 채팅 rate-limit에서 분리/완화
- `GameRoomEventHandler.handleJoin`
  - 현재는 join 알림 playerInfo를 재생성함
  - 변경 후에는 Redis 저장값 재사용 또는 이벤트 payload 확장으로 필드 유실 방지

---

## 6) 현재 정책과의 충돌 검토 (반드시 확인)
- 현재 `SessionDisconnectEvent`는 기본적으로 `leaveGameRoomUseCase`를 호출해 방에서 제거한다.
- 즉, disconnect 시에는 `DISCONNECTED` 상태 표기보다 `PLAYER_LEFT`가 먼저/주로 발생한다.

실무 권고:
- 이번 이슈(P0)에서는 `RESULT/ROOM/IN_GAME` 실시간 동기화에 집중
- `DISCONNECTED`는 "방에 남긴 채 연결만 끊김" 정책이 필요할 때 별도 이슈로 분리

---

## 7) Vue.js 프론트 반영 가이드

## 7-1. store 병합 규칙 (Pinia 권장)
- `PLAYER_LIST_UPDATED` 수신: 전체 동기화
- `SCREEN_STATE_UPDATED` 수신: 해당 member만 갱신
- member별 `screenStateSeq` 역전 이벤트 무시

## 7-2. 전송 타이밍
- 결과창 진입 직후: `RESULT` 전송
- 방 복귀 버튼 클릭 직후: `ROOM` 전송
- Room View mount 시: `ROOM` 1회 재전송(멱등)

## 7-3. UI 문구
- RESULT: `상대가 결과 화면에 있습니다`
- ROOM: `상대가 방으로 돌아왔습니다`
- IN_GAME: `상대가 게임 화면에 있습니다`

---

## 8) 단계별 실행 계획

### P0 (핵심 기능)
- playerInfo 필드/enum 추가
- 상태 업데이트 MessageMapping + UseCase 추가
- Redis 원자 갱신(Lua) + `SCREEN_STATE_UPDATED` 브로드캐스트
- Vue 결과창 상대 상태 표기 반영

완료 조건:
- 2인 room에서 `RESULT -> ROOM` 전환이 상대 UI에 1초 내 반영

### P1 (정합성 강화)
- join 이벤트 알림 객체 재생성 제거(저장값 기준으로 통일)
- stale drop 메트릭 추가
- rate-limit 분리 정책 정교화

완료 조건:
- 지연/중복 전송에서도 상태 역전 없음

### P2 (운영 고도화)
- 필요 시 DISCONNECTED 정책 재설계
- 부하테스트 및 알람 임계치 확정

---

## 9) 테스트 체크리스트
- seq 역전 drop 테스트 (`newSeq < currentSeq`)
- 동일 seq 멱등 테스트
- `SCREEN_STATE_UPDATED` 단건 반영 테스트
- `PLAYER_LIST_UPDATED` 전체 복구 테스트
- Vue store 병합 테스트 (delta 후 full sync, full sync 후 delta)

---

## 10) 최종 코멘트
- "playerList 통합" 전략은 현재 구조에 가장 잘 맞는다.
- 다만 이번 코드베이스에서는 **Redis 원자 갱신(Lua) 없이 seq 정합성을 보장할 수 없다**.
- 따라서 P0 구현의 성패는 `GameRoomRedisRepository`의 조건부 갱신 추가에 달려 있다.
