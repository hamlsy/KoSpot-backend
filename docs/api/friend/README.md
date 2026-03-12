# Friend API & WebSocket Guide

친구 기능 연동을 위한 REST API 및 WebSocket 메시지 양식 문서입니다.

> 모든 경로는 `/api` prefix 없이 작성되어 있습니다.

---

## 1) 기본 정보

### 인증
- REST: `Authorization: Bearer {accessToken}`
- WebSocket(STOMP): CONNECT 헤더에 `Authorization: Bearer {accessToken}`

### 공통 응답 포맷 (REST)
`ApiResponseDto<T>`

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공",
  "result": {}
}
```

---

## 2) REST API

## 2.1 내 친구 목록 조회

`GET /friends`

내 친구들의 요약 정보를 조회합니다.

Response `result`: `FriendListResponse[]`

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공",
  "result": [
    {
      "friendMemberId": 23,
      "nickname": "kospot_user",
      "equippedMarkerImageUrl": "https://cdn.kospot.com/marker/basic.png",
      "online": true,
      "roadViewRankTier": "GOLD",
      "roadViewRankLevel": "THREE",
      "roadViewRatingScore": 1570
    }
  ]
}
```

응답 필드 설명
- `friendMemberId`: 친구 회원 ID
- `nickname`: 친구 닉네임
- `equippedMarkerImageUrl`: 친구 장착 마커 이미지 URL
- `online`: WebSocket 연결 상태가 `CONNECTED`인 경우 `true`, 그 외는 `false`
- `roadViewRankTier`: 로드뷰 티어
- `roadViewRankLevel`: 로드뷰 레벨
- `roadViewRatingScore`: 로드뷰 레이팅 포인트

---

### 2.2 친구 요청 보내기

`POST /friends/requests`

특정 회원에게 친구 요청을 보냅니다.

Request
```json
{
  "receiverMemberId": 23
}
```

Response `result`: `FriendRequestActionResponse`

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공",
  "result": {
    "requestId": 101,
    "status": "PENDING"
  }
}
```

---

### 2.3 친구 요청 승인

`PATCH /friends/requests/{requestId}/approve`

받은 친구 요청을 승인합니다.

Path
- `requestId` (Long): 친구 요청 ID

Response `result`: `FriendRequestActionResponse`

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공",
  "result": {
    "requestId": 101,
    "status": "ACTIVE"
  }
}
```

---

### 2.4 받은 친구 요청 목록 조회

`GET /friends/requests/incoming`

Query
- `page` (int, default: 0)
- `size` (int, optional, default: 20, max: 50)

Response `result`: `IncomingFriendRequestResponse[]`

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공",
  "result": [
    {
      "requestId": 101,
      "senderMemberId": 11,
      "senderNickname": "map_runner",
      "senderMarkerImageUrl": "https://cdn.kospot.com/marker/fire.png",
      "requestedAt": "2026-02-28T14:10:00"
    }
  ]
}
```

---

### 2.5 친구 삭제

`DELETE /friends/{friendMemberId}`

친구 관계를 삭제합니다.

Path
- `friendMemberId` (Long): 친구 회원 ID

Response: 성공 응답(별도 result 없음)

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공"
}
```

---

### 2.6 친구 채팅방 조회/생성

`GET /friends/{friendMemberId}/chat-room`

특정 친구와의 1:1 채팅방을 조회하고, 없으면 생성합니다.

Path
- `friendMemberId` (Long): 친구 회원 ID

Response `result`: `FriendChatRoomResponse`

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공",
  "result": {
    "roomId": 301,
    "friendMemberId": 23
  }
}
```

---

### 2.7 친구 채팅 메시지 전송

`POST /friends/chat-rooms/{roomId}/messages`

친구 채팅방에 메시지를 저장합니다. (현재 DB 저장)

Path
- `roomId` (Long): 친구 채팅방 ID

Request
```json
{
  "content": "안녕하세요!"
}
```

Response `result`: `FriendChatMessageResponse`

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공",
  "result": {
    "messageId": "0f05f1ea-1767-4f96-9f04-b3bb2b0f76ca",
    "senderMemberId": 11,
    "content": "안녕하세요!",
    "createdAt": "2026-02-28T14:30:00"
  }
}
```

---

### 2.8 친구 채팅 메시지 목록 조회

`GET /friends/chat-rooms/{roomId}/messages`

친구 채팅 메시지를 최신순으로 조회합니다.

Path
- `roomId` (Long): 친구 채팅방 ID

Query
- `page` (int, default: 0)
- `size` (int, optional, default: 30, max: 100)

Response `result`: `FriendChatMessageResponse[]`

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공",
  "result": [
    {
      "messageId": "0f05f1ea-1767-4f96-9f04-b3bb2b0f76ca",
      "senderMemberId": 11,
      "content": "안녕하세요!",
      "createdAt": "2026-02-28T14:30:00"
    },
    {
      "messageId": "52f7f79f-36fb-42c4-9535-6db6db2de7f7",
      "senderMemberId": 23,
      "content": "반가워요",
      "createdAt": "2026-02-28T14:29:10"
    }
  ]
}
```

---

## 3) WebSocket(STOMP) 양식

### 3.1 친구 요청 알림 수신 (현재 구현)

친구 요청 생성 시, 수신자는 기존 개인 알림 채널에서 실시간 알림을 받습니다.

- 구독 채널: `SUBSCRIBE /user/queue/notification`
- 타입: `FRIEND_REQUEST`

Payload 예시

```json
{
  "notificationId": 812,
  "type": "FRIEND_REQUEST",
  "title": "새 친구 요청",
  "content": null,
  "payloadJson": "{\"friendRequestId\":101,\"senderMemberId\":11}",
  "sourceId": 101,
  "isRead": false,
  "createdAt": "2026-02-28T14:10:00"
}
```

`payloadJson` 파싱 예시

```json
{
  "friendRequestId": 101,
  "senderMemberId": 11
}
```

---

### 3.2 친구 채팅 WebSocket 양식 (명세 초안)

> 현재 친구 채팅은 REST로 저장/조회됩니다.
> 아래는 프론트/백엔드 협업을 위한 WebSocket 메시지 양식 초안입니다.

채널 제안
- 전송: `SEND /app/friends.chat.send`
- 구독: `SUBSCRIBE /topic/friends/chat-room/{roomId}`

전송 payload 양식

```json
{
  "roomId": 301,
  "content": "다음 판 같이 하실래요?"
}
```

브로드캐스트 payload 양식

```json
{
  "roomId": 301,
  "messageId": "6d67c58a-26f4-4688-8f5f-b20b54f7cd2f",
  "senderMemberId": 11,
  "content": "다음 판 같이 하실래요?",
  "createdAt": "2026-02-28T14:42:00"
}
```

---

## 4) 프론트 연동 권장 순서

1. 친구 목록 로드: `GET /friends`
2. 받은 요청 로드: `GET /friends/requests/incoming?page=0`
3. 개인 알림 채널 구독: `/user/queue/notification`
4. 친구 선택 시 채팅방 조회/생성: `GET /friends/{friendMemberId}/chat-room`
5. 채팅 이력 로드: `GET /friends/chat-rooms/{roomId}/messages?page=0`
6. 메시지 전송: `POST /friends/chat-rooms/{roomId}/messages`

---

## 5) 주의사항

- 친구 요청 승인 API만 현재 제공됩니다. (거절 API는 미구현)
- 친구 채팅은 현재 DB 저장 기반이며, Redis/배치 적재는 추후 확장 예정입니다.
- 친구 알림은 Notification API와 동일한 채널/스키마를 사용합니다.
