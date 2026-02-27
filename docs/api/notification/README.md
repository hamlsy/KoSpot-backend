# Notification API & WebSocket Guide

프론트엔드에서 알림 기능(목록/읽음/실시간 수신)을 바로 구현할 수 있도록 정리한 문서입니다.

---

## 1) 기본 정보

### 저장 정책
- 알림은 DB에 영구 저장하지 않습니다.
- Redis에 저장되며 TTL은 `14일(2주)` 입니다.
- 2주가 지난 알림은 목록에서 자동으로 사라질 수 있습니다.

### 인증
- REST: `Authorization: Bearer {accessToken}`
- WebSocket(STOMP): CONNECT 헤더에 `Authorization: Bearer {accessToken}`

### 알림 타입
- `ADMIN_MESSAGE`: 관리자 메시지
- `NOTICE`: 공지사항 업로드 알림
- `FRIEND_REQUEST`: 친구 요청 알림(친구 기능은 추후 연동)

### 공통 응답 포맷(REST)
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

### 2.1 내 알림 목록 조회

`GET /notifications`

Query
- `page` (int, default: 0)
- `size` (int, optional, default: 20, max: 50)
- `type` (string, optional): `ADMIN_MESSAGE|NOTICE|FRIEND_REQUEST`
- `isRead` (boolean, optional)

Response `result`: `NotificationItem[]`

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공",
  "result": [
    {
      "notificationId": 10,
      "receiverMemberId": 3,
      "type": "NOTICE",
      "title": "2/26 업데이트 안내",
      "content": null,
      "payloadJson": "{\"noticeId\":123}",
      "sourceId": 123,
      "isRead": false,
      "readAt": null,
      "createdAt": "2026-02-26T10:30:00"
    }
  ]
}
```

프론트 구현 메모
- 정렬: 서버 최신순(`createdDate desc`)
- 무한 스크롤/더보기라면 `page` 증가 방식으로 호출

### 2.2 미읽음 알림 개수

`GET /notifications/unread-count`

Response `result`
```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공",
  "result": {
    "unreadCount": 5
  }
}
```

### 2.3 단건 읽음 처리

`PATCH /notifications/{notificationId}/read`

Response: 성공 응답(별도 result 없음)

프론트 구현 메모
- 리스트에서 해당 알림 `isRead=true`, `readAt`은 서버에서만 관리(프론트는 즉시 반영)

### 2.4 전체 읽음 처리

`PATCH /notifications/read-all`

Response `result`
```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공",
  "result": {
    "updatedCount": 12
  }
}
```

---

## 3) WebSocket(STOMP) - 실시간 알림

### 3.1 연결

엔드포인트
- SockJS(STOMP): `ws://{host}:{port}/ws`
- (선택) 알림 전용 SockJS: `ws://{host}:{port}/ws/notification`

권장
- 별도 연결 없이 `/ws` 하나로 연결 후, 아래 채널을 구독하는 방식으로 구현

CONNECT 예시
```javascript
const socket = new SockJS('/ws');
const client = Stomp.over(socket);

client.connect(
  { Authorization: 'Bearer ' + accessToken },
  () => {
    subscribeNotificationChannels(client);
  },
  (err) => console.error('stomp error', err)
);
```

### 3.2 구독 채널

전역 알림(공지사항/시스템)
- `SUBSCRIBE /topic/notification/global`

개인 알림(관리자 메시지/친구 요청 등)
- `SUBSCRIBE /user/queue/notification`

구독 예시
```javascript
function subscribeNotificationChannels(client) {
  client.subscribe('/topic/notification/global', (msg) => {
    const payload = JSON.parse(msg.body);
    onGlobalNotification(payload);
  });

  client.subscribe('/user/queue/notification', (msg) => {
    const payload = JSON.parse(msg.body);
    onPersonalNotification(payload);
  });
}
```

### 3.3 WebSocket payload 스키마

`NotificationMessage`

```json
{
  "notificationId": 10,
  "type": "ADMIN_MESSAGE",
  "title": "관리자 공지",
  "content": "내용...",
  "payloadJson": "{\"adminId\":1}",
  "sourceId": null,
  "isRead": false,
  "createdAt": "2026-02-26T10:30:00"
}
```

필드 설명
- `notificationId`: Redis에 저장된 알림 ID. 전역 브로드캐스트(공지) 일부는 null일 수 있음
- `type`: 알림 타입
- `title/content`: UI 표시용
- `payloadJson`: 타입별 추가 데이터(JSON 문자열)
- `sourceId`: Notice ID / FriendRequest ID 등

타입별 payloadJson 예시
- `NOTICE`: `{ "noticeId": 123 }`
- `ADMIN_MESSAGE`: `{ "adminId": 1 }`
- `FRIEND_REQUEST`: `{ "friendRequestId": 55, "senderMemberId": 10 }`

### 3.4 프론트 동기화 전략(중요)

WebSocket은 네트워크/백그라운드 상태에 따라 유실이 발생할 수 있으므로, 아래를 권장합니다.

1) 앱 진입/재연결 시
- `GET /notifications/unread-count`
- `GET /notifications?page=0`

2) WebSocket 메시지 수신 시
- UI에 즉시 반영(리스트 prepend + badge 증가)
- 필요하면 `notificationId`로 read 처리 호출

---

## 4) 관리자 메시지 발송(백오피스/관리자 페이지)

`POST /admin/notifications/messages`

Request
```json
{
  "targetType": "ALL",
  "memberIds": null,
  "title": "긴급 공지",
  "content": "점검 안내..."
}
```

`targetType`
- `ALL`: 전체 발송(전역 푸시 + 전체 사용자 Redis 알림 저장)
- `MEMBERS`: 특정 사용자 발송(개인 푸시 + 대상 사용자 Redis 알림 저장)

Response `result`
```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공",
  "result": {
    "sentCount": 100
  }
}
```
