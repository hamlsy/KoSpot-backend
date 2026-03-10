# 멀티 GameRoom 화면 상태 동기화 프론트 연동 명세 (JOINING 게이트 반영)

> 최신 백엔드 구현 기준 문서. 기존 분리 채널/스냅샷 방식이 아니라 `playerList` 채널 통합 방식이다.

## 1) 목적
- Result View에서 상대 화면 상태(`IN_GAME`, `RESULT`, `ROOM`)를 실시간 반영한다.
- 조인 직후 실제 방 채널에 들어오기 전(`JOINING`)에는 게임 시작을 차단한다.
- 방 화면 진입 여부를 서버가 신뢰할 수 있게 subscribe 이벤트와 연동한다.

## 2) 핵심 변경점 요약
- 서버는 조인 직후 플레이어를 `JOINING`으로 저장한다.
- 프론트가 `/topic/room/{roomId}/playerList`를 구독하면 서버가 `JOINING -> ROOM`으로 승격한다.
- 방장 시작 API(`/rooms/{roomId}/start`)는 시작 전 전원 `ROOM`인지 검증한다.
- 화면 상태 delta/full sync 모두 기존 `playerList` 채널로 받는다.

## 3) 프론트가 반드시 해야 하는 연결

### 3.1 방 진입 시 필수 순서
1. REST 조인 호출: `POST /rooms/{roomId}/join`
2. WebSocket 연결 확인
3. 즉시 구독: `SUBSCRIBE /topic/room/{roomId}/playerList`
4. 구독 성공 후 플레이어 리스트/상태 store 반영

중요:
- 3단계를 늦추면 본인 상태가 `JOINING`에 머물 수 있고, 방장이 시작 요청 시 서버에서 거절된다.

### 3.2 화면 상태 전송
클라이언트 전송 엔드포인트:
- `SEND /app/room.{roomId}.screen.state`

요청 예시:
```json
{
  "state": "RESULT",
  "clientSeq": 12,
  "clientTimestamp": 1760000000000
}
```

필드 규칙:
- `state`: `IN_GAME | RESULT | ROOM`만 전송 (`JOINING`은 서버 관리 상태)
- `clientSeq`: 같은 세션 내 단조 증가
- `clientTimestamp`: epoch ms

## 4) 서버 수신 채널/메시지 포맷

구독 채널:
- `SUBSCRIBE /topic/room/{roomId}/playerList`

메시지 공통 포맷:
```json
{
  "type": "SCREEN_STATE_UPDATED",
  "roomId": "123",
  "playerInfo": {
    "memberId": 456,
    "nickname": "player",
    "markerImageUrl": "...",
    "isHost": false,
    "screenState": "ROOM",
    "screenStateSeq": 12,
    "screenStateUpdatedAt": 1760000000456
  },
  "players": null,
  "timestamp": 1760000000456
}
```

주요 `type`:
- `SCREEN_STATE_UPDATED`: 특정 플레이어 상태 delta
- `PLAYER_LIST_UPDATED`: 전체 플레이어 스냅샷(`players` 사용)
- `PLAYER_JOINED`, `PLAYER_LEFT`, `PLAYER_KICKED`, `HOST_CHANGED`도 동일 채널로 수신

## 5) 화면별 전송 타이밍

### 5.1 게임 화면 진입
- `sendScreenState(roomId, "IN_GAME")`

### 5.2 결과 화면 진입
- Result View mount 직후 `sendScreenState(roomId, "RESULT")`

### 5.3 결과 -> 방 복귀
- 복귀 버튼 클릭 시 `sendScreenState(roomId, "ROOM")`
- Room View mount 후 `ROOM` 1회 재전송 권장(멱등)

## 6) 시작 버튼 UX 규칙 (중요)

방장 UI에서 시작 버튼 처리:
- `playerList`의 모든 플레이어 `screenState === "ROOM"`일 때만 활성화 권장
- 1명이라도 `JOINING`이면 비활성 + 안내 문구 노출

권장 문구:
- `참여 중인 플레이어가 있습니다. 잠시 후 다시 시도해주세요.`

백엔드도 동일 검증을 하므로, 프론트에서 미리 막아 불필요한 에러 호출을 줄인다.

## 7) 상태 저장(Store) 규칙

권장 타입:
```ts
type ScreenState = "JOINING" | "IN_GAME" | "RESULT" | "ROOM" | "DISCONNECTED";

interface PlayerState {
  memberId: number;
  screenState: ScreenState;
  screenStateSeq: number;
  screenStateUpdatedAt: number;
  nickname: string;
  markerImageUrl: string;
  isHost: boolean;
}
```

병합 규칙:
- `PLAYER_LIST_UPDATED`: room 상태를 전체 교체
- `SCREEN_STATE_UPDATED`: 해당 `memberId`만 부분 갱신
- delta 적용 시 `incoming.screenStateSeq < current.screenStateSeq`이면 drop
- `incoming.screenStateSeq == current.screenStateSeq`일 때
  - `screenState`가 같으면 no-op
  - `screenState`가 다르면 반영 (서버 승격 `JOINING -> ROOM` 케이스 대응)

## 8) 재연결 시 처리

재연결 성공 직후:
1. `/topic/room/{roomId}/playerList` 재구독
2. 현재 페이지 상태 재전송 (`IN_GAME` 또는 `RESULT` 또는 `ROOM`)

참고:
- 현재 백엔드는 별도 `screen.state.snapshot` 엔드포인트를 제공하지 않는다.
- 복구 기준은 `PLAYER_LIST_UPDATED` + 재전송 조합이다.

## 9) 프론트 구현 예시 (TypeScript)

```typescript
type ClientState = "IN_GAME" | "RESULT" | "ROOM";

class RoomScreenStateSync {
  private seq = 0;

  constructor(private stomp: any, private roomId: string) {}

  subscribePlayerList(onMessage: (payload: any) => void) {
    return this.stomp.subscribe(`/topic/room/${this.roomId}/playerList`, (frame: any) => {
      onMessage(JSON.parse(frame.body));
    });
  }

  sendState(state: ClientState) {
    this.seq += 1;
    this.stomp.publish({
      destination: `/app/room.${this.roomId}.screen.state`,
      body: JSON.stringify({
        state,
        clientSeq: this.seq,
        clientTimestamp: Date.now(),
      }),
    });
  }
}
```

## 10) QA 체크리스트
- 조인 후 `playerList` 구독 직후 본인 상태가 `JOINING -> ROOM`으로 바뀌는가
- 방장 시작 시 `JOINING` 플레이어가 있으면 시작이 차단되는가
- Result 진입 시 상대에게 `RESULT`가 반영되는가
- Room 복귀 시 상대에게 `ROOM`이 반영되는가
- 재연결 후 재구독 + 상태 재전송으로 최종 상태가 복구되는가
