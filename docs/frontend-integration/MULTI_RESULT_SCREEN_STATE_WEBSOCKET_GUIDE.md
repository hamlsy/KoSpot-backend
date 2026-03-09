# 멀티 결과창 상대 화면 상태 WebSocket 프론트 연동 명세

> 게임 종료 후 Result View에서 상대가 Room View로 돌아왔는지 실시간으로 표시하기 위한 프론트엔드 구현 명세서

## 1. 목적
- Result View에서 상대 플레이어의 현재 화면 상태(`RESULT`, `ROOM`, `IN_GAME`, `DISCONNECTED`)를 실시간으로 반영한다.
- 이벤트 유실/재연결 상황에서도 스냅샷 요청으로 최종 상태를 복구한다.

## 2. 프론트 변경 범위

### 2.1 추가해야 하는 것
- 화면 상태 전용 WebSocket 채널 구독 로직
- 화면 상태 업데이트 전송 함수 (`sendScreenState`)
- 초기 스냅샷 요청 함수 (`requestScreenStateSnapshot`)
- 상태 시퀀스(`clientSeq`) 관리 로직
- Result View 상대 상태 뱃지/문구 UI

### 2.2 수정해야 하는 것
- 게임 종료 시점 라우팅 로직: Result View 진입 직후 상태 이벤트 전송
- 결과창 -> 방 복귀 버튼 핸들러: Room View 이동 직전/직후 상태 이벤트 전송
- WebSocket 재연결 로직: 재연결 후 채널 재구독 + 스냅샷 재요청
- 전역 스토어(또는 페이지 스토어): `roomId + memberId` 기준 상태 맵 추가

---

## 3. 채널/메시지 명세

## 3.1 서버로 전송 (Client -> Server)

### 화면 상태 업데이트
`
SEND /app/room.{roomId}.screen.state
`

요청 바디:
```json
{
  "state": "RESULT",
  "clientSeq": 12,
  "clientTimestamp": 1760000000000
}
```

필드:
- `state`: `IN_GAME | RESULT | ROOM`
- `clientSeq`: 세션 내 단조 증가 정수 (필수)
- `clientTimestamp`: 클라이언트 epoch ms (옵션)

주의:
- `memberId`는 보내지 않는다. 서버는 JWT Principal에서 식별한다.
- `clientSeq`는 페이지 새로고침 시 0부터 재시작 가능(세션 기준).

### 스냅샷 요청
`
SEND /app/room.{roomId}.screen.state.snapshot
`

요청 바디: 없음 또는 `{}`

## 3.2 서버에서 수신 (Server -> Client)

### 상태 변경 브로드캐스트
`
SUBSCRIBE /topic/game/{roomId}/screen/state
`

메시지 예시:
```json
{
  "roomId": "123",
  "memberId": 456,
  "state": "ROOM",
  "clientSeq": 13,
  "revision": 87,
  "serverTimestamp": 1760000000456
}
```

### 스냅샷 응답 (개인 큐)
`
SUBSCRIBE /user/queue/game/{roomId}/screen/state/snapshot
`

메시지 예시:
```json
{
  "roomId": "123",
  "revision": 87,
  "serverTimestamp": 1760000000500,
  "states": [
    {
      "memberId": 123,
      "state": "RESULT",
      "clientSeq": 12,
      "updatedAt": 1760000000000
    },
    {
      "memberId": 456,
      "state": "ROOM",
      "clientSeq": 13,
      "updatedAt": 1760000000450
    }
  ]
}
```

---

## 4. 화면별 전송 타이밍 명세

## 4.1 IN_GAME -> RESULT
- 트리거: 게임 종료 후 결과창 렌더링 직후
- 동작: `sendScreenState(roomId, "RESULT")`

## 4.2 RESULT -> ROOM
- 트리거: 결과창의 "방으로 돌아가기" 액션
- 동작: 라우팅 직전 `ROOM` 전송, 실패해도 라우팅 진행
- 권장: Room View 진입 후 한 번 더 `ROOM` 재전송(멱등)

## 4.3 Room 체류
- 트리거: Room View 최초 진입
- 동작: `sendScreenState(roomId, "ROOM")`

## 4.4 비정상 종료/새로고침
- `beforeunload`에서 best-effort 전송은 시도 가능
- 실패해도 서버 disconnect 보정(`DISCONNECTED`)을 신뢰

---

## 5. 상태 저장(Store) 명세

권장 전역 상태 구조:
```ts
type ScreenState = "IN_GAME" | "RESULT" | "ROOM" | "DISCONNECTED";

interface MemberScreenState {
  memberId: number;
  state: ScreenState;
  clientSeq: number;
  updatedAt: number;
}

interface RoomScreenStateStore {
  roomId: string;
  revision: number;
  byMemberId: Record<number, MemberScreenState>;
  lastSyncedAt: number;
}
```

클라이언트 적용 규칙:
- 동일 `memberId`의 기존 상태보다 `clientSeq` 작으면 무시
- 동일 `clientSeq`면 no-op
- 스냅샷 수신 시 `revision`이 현재보다 크거나 같으면 전체 동기화

---

## 6. UI 반영 명세 (Result View)

상대 상태 표시 규칙:
- `RESULT`: "상대가 결과 화면에 있습니다"
- `ROOM`: "상대가 방으로 돌아왔습니다"
- `IN_GAME`: "상대가 아직 게임 화면에 있습니다"
- `DISCONNECTED`: "상대 연결이 일시 끊겼습니다"

권장 UI:
- 상대 상태 텍스트 + 상태 점(dot) 컬러
- `ROOM` 수신 시 버튼 강조: "상대가 돌아왔어요. 방으로 이동"

---

## 7. 재연결/복구 명세

재연결 성공 시 순서:
1. `/topic/game/{roomId}/screen/state` 재구독
2. `/user/queue/game/{roomId}/screen/state/snapshot` 재구독
3. 스냅샷 재요청 전송
4. 현재 화면 상태 재전송 (`RESULT` 또는 `ROOM`)

복구 원칙:
- 실시간 이벤트만 신뢰하지 말고 재연결 시 항상 스냅샷으로 보정

---

## 8. 프론트 구현 예시 (TypeScript)

```typescript
class ScreenStateSync {
  private seq = 0;

  constructor(private stomp: any, private roomId: string) {}

  nextSeq(): number {
    this.seq += 1;
    return this.seq;
  }

  sendState(state: "IN_GAME" | "RESULT" | "ROOM") {
    this.stomp.publish({
      destination: `/app/room.${this.roomId}.screen.state`,
      body: JSON.stringify({
        state,
        clientSeq: this.nextSeq(),
        clientTimestamp: Date.now(),
      }),
    });
  }

  requestSnapshot() {
    this.stomp.publish({
      destination: `/app/room.${this.roomId}.screen.state.snapshot`,
      body: "{}",
    });
  }

  subscribe(onChanged: (msg: any) => void, onSnapshot: (msg: any) => void) {
    this.stomp.subscribe(`/topic/game/${this.roomId}/screen/state`, (frame: any) => {
      onChanged(JSON.parse(frame.body));
    });

    this.stomp.subscribe(`/user/queue/game/${this.roomId}/screen/state/snapshot`, (frame: any) => {
      onSnapshot(JSON.parse(frame.body));
    });
  }
}
```

---

## 9. QA 체크리스트
- 결과창 진입 시 상대에게 `RESULT`가 즉시 보이는가
- 방 복귀 시 상대에게 `ROOM`이 즉시 보이는가
- 네트워크 끊김 후 재연결 시 스냅샷으로 상태가 복구되는가
- 중복 클릭/중복 전송에도 UI가 흔들리지 않는가(멱등)
- 본인 상태와 상대 상태를 구분해 표시하는가

---

## 10. 프론트 작업 티켓 분리 권장
- FE-1: 상태 채널 구독/전송 유틸 구현
- FE-2: Result View 상태 UI 컴포넌트 추가
- FE-3: clientSeq/store/revision 반영
- FE-4: 재연결 + 스냅샷 복구 처리
- FE-5: E2E 시나리오 테스트 (2인 멀티)

이 문서 기준으로 구현하면 프론트는 최소 변경으로 실시간 상태 표시를 달성하면서, 재연결/유실 상황에서도 안정적으로 상태를 복구할 수 있다.
