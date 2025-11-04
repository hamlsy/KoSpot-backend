# 글로벌 로비 WebSocket API 명세서

글로벌 로비 채팅 및 실시간 통신 관련 WebSocket API 문서입니다.

---

## 📋 목차

- [연결 설정](#연결-설정)
- [로비 입장/퇴장](#로비-입장퇴장)
- [로비 채팅](#로비-채팅)
- [메시지 타입](#메시지-타입)
- [에러 처리](#에러-처리)

---

## 연결 설정

### WebSocket 엔드포인트
```
ws://localhost:8080/ws
```

### SockJS를 사용한 연결 예시 (JavaScript)
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// JWT 토큰을 헤더에 포함하여 연결
stompClient.connect({
  'Authorization': 'Bearer ' + accessToken
}, function(frame) {
  console.log('Connected: ' + frame);
});
```

**참고사항**
- WebSocket 연결 시 `Authorization` 헤더에 JWT 토큰이 필수입니다.
- STOMP 프로토콜을 사용합니다.
- SockJS를 통한 fallback이 지원됩니다.

---

## 로비 입장/퇴장

### 1. 로비 입장

**전송 경로**
```
/app/chat.join.lobby
```

**전송 방법**
```javascript
// 페이로드 없이 메시지만 전송
stompClient.send('/app/chat.join.lobby', {}, JSON.stringify({}));
```

**처리 내용**
- 세션 ID와 회원 ID를 Redis에 저장하여 로비 접속 상태 관리
- 로비 유저 카운트 증가

**Response**
- 별도의 응답 메시지 없음 (서버에서 로그 기록만 수행)

---

### 2. 로비 퇴장

**전송 경로**
```
/app/chat.leave.lobby
```

**전송 방법**
```javascript
// 페이로드 없이 메시지만 전송
stompClient.send('/app/chat.leave.lobby', {}, JSON.stringify({}));
```

**처리 내용**
- Redis에서 세션 정보 삭제
- 로비 유저 카운트 감소

**Response**
- 별도의 응답 메시지 없음 (서버에서 로그 기록만 수행)

---

## 로비 채팅

### 1. 채팅 메시지 전송

**전송 경로**
```
/app/chat.message.lobby
```

**구독 경로 (수신용)**
```
/topic/lobby
```

**전송 메시지 형식**
```json
{
  "content": "안녕하세요!"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| content | String | O | 채팅 메시지 내용 (비어있으면 안됨) |

**전송 예시 (JavaScript)**
```javascript
// 1. 먼저 구독 설정
stompClient.subscribe('/topic/lobby', function(message) {
  const data = JSON.parse(message.body);
  console.log('받은 메시지:', data);
  // UI 업데이트 로직
});

// 2. 메시지 전송
stompClient.send('/app/chat.message.lobby', {}, JSON.stringify({
  content: '안녕하세요!'
}));
```

---

### 2. 채팅 메시지 수신

**구독 경로**
```
/topic/lobby
```

**수신 메시지 형식**
```json
{
  "senderId": 123,
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "nickname": "홍길동",
  "content": "안녕하세요!",
  "messageType": "GLOBAL_CHAT",
  "timestamp": "2025-11-04T10:30:00"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| senderId | Long | 메시지 발신자의 회원 ID |
| messageId | String | 메시지 고유 ID (UUID) - 중복 메시지 방지용 |
| nickname | String | 발신자 닉네임 |
| content | String | 채팅 메시지 내용 |
| messageType | String | 메시지 타입 (항상 "GLOBAL_CHAT") |
| timestamp | DateTime | 메시지 전송 시각 (ISO 8601 형식) |

**수신 예시 (JavaScript)**
```javascript
stompClient.subscribe('/topic/lobby', function(message) {
  const chatMessage = JSON.parse(message.body);
  
  // 메시지 표시
  displayMessage({
    sender: chatMessage.nickname,
    content: chatMessage.content,
    time: new Date(chatMessage.timestamp),
    messageId: chatMessage.messageId
  });
});
```

---

## 메시지 타입

로비 채팅에서 사용되는 `messageType`은 다음과 같습니다:

| 타입 | 값 | 설명 |
|------|------|------|
| 일반 채팅 | `GLOBAL_CHAT` | 글로벌 로비 채팅 메시지 |
| 시스템 메시지 | `SYSTEM_CHAT` | 시스템 알림 (입장/퇴장 등) |
| 공지사항 | `NOTICE_CHAT` | 공지사항 메시지 |

---

## 에러 처리

### Rate Limiting
- 1분에 최대 40개의 메시지 전송 가능
- 제한 초과 시 추가 메시지는 무시됩니다.

### 중복 메시지 방지
- 서버는 `messageId` 기반으로 5분간 중복 메시지를 감지합니다.
- 중복된 `messageId`의 메시지는 처리되지 않습니다.

### 연결 오류
```javascript
stompClient.connect({
  'Authorization': 'Bearer ' + accessToken
}, function(frame) {
  console.log('Connected: ' + frame);
}, function(error) {
  console.error('Connection error:', error);
  // 재연결 로직
});
```

---

## 전체 사용 예시 (JavaScript)

```javascript
// WebSocket 연결
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// 연결 수립
stompClient.connect({
  'Authorization': 'Bearer ' + accessToken
}, function(frame) {
  console.log('WebSocket Connected');
  
  // 1. 로비 입장
  stompClient.send('/app/chat.join.lobby', {}, JSON.stringify({}));
  
  // 2. 채팅 메시지 구독
  stompClient.subscribe('/topic/lobby', function(message) {
    const data = JSON.parse(message.body);
    console.log(`[${data.nickname}]: ${data.content}`);
  });
  
  // 3. 메시지 전송
  function sendMessage(content) {
    stompClient.send('/app/chat.message.lobby', {}, JSON.stringify({
      content: content
    }));
  }
  
  // 사용 예시
  sendMessage('안녕하세요!');
});

// 연결 종료 시
window.addEventListener('beforeunload', function() {
  stompClient.send('/app/chat.leave.lobby', {}, JSON.stringify({}));
  stompClient.disconnect();
});
```

---

## 주의사항

1. **인증 필수**: WebSocket 연결 시 반드시 유효한 JWT 토큰이 필요합니다.
2. **구독 우선**: 메시지를 전송하기 전에 먼저 `/topic/lobby`를 구독해야 응답을 받을 수 있습니다.
3. **로비 입장**: 채팅을 사용하기 전에 `/app/chat.join.lobby`를 호출하여 로비에 입장해야 합니다.
4. **정리 작업**: 페이지 이탈 시 반드시 `/app/chat.leave.lobby`를 호출하여 세션을 정리해야 합니다.
5. **중복 방지**: 클라이언트에서도 `messageId`를 추적하여 중복 메시지를 필터링하는 것을 권장합니다.

