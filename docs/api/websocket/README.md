# WebSocket API 문서

KoSpot 멀티플레이 게임의 실시간 통신을 위한 WebSocket API 문서입니다.

---

## 📋 문서 구성

### [1. 글로벌 로비 WebSocket](./LOBBY_WEBSOCKET.md)
글로벌 로비 채팅 및 실시간 통신 관련 API

**주요 기능**
- 로비 입장/퇴장
- 로비 채팅
- 실시간 메시지 수신

**핵심 엔드포인트**
- 메시지 전송: `/app/chat.message.lobby`
- 메시지 구독: `/topic/lobby`
- 로비 입장: `/app/chat.join.lobby`
- 로비 퇴장: `/app/chat.leave.lobby`

---

### [2. 게임방 WebSocket](./GAMEROOM_WEBSOCKET.md)
게임방 채팅, 플레이어 목록 관리 및 실시간 알림 관련 API

**주요 기능**
- 게임방 채팅 (일반/팀)
- 플레이어 목록 실시간 동기화
- 팀 변경
- 플레이어 입장/퇴장/강퇴 알림
- 게임 시작 알림

**핵심 엔드포인트**
- 채팅 전송: `/app/room.{roomId}.chat`
- 채팅 구독: `/topic/room/{roomId}/chat`
- 플레이어 목록 구독: `/topic/room/{roomId}/playerList`
- 팀 변경: `/app/room.{roomId}.switchTeam`

---

## 🔌 WebSocket 연결 설정

### 연결 엔드포인트
```
ws://localhost:8080/ws
```

### 프로토콜
- **STOMP** (Simple Text Oriented Messaging Protocol)
- **SockJS** fallback 지원

### 인증
모든 WebSocket 연결 시 JWT 토큰이 필수입니다.

```javascript
stompClient.connect({
  'Authorization': 'Bearer ' + accessToken
}, onConnected, onError);
```

---

## 📡 채널 패턴

### 글로벌 채널
| 채널 | 설명 |
|------|------|
| `/topic/lobby` | 글로벌 로비 채팅 |
| `/topic/chat/lobby` | 로비 채팅 (Deprecated) |

### 게임방 채널
| 채널 패턴 | 설명 |
|----------|------|
| `/topic/room/{roomId}/chat` | 게임방 채팅 |
| `/topic/room/{roomId}/playerList` | 플레이어 목록 변경 알림 |
| `/topic/room/{roomId}/settings` | 방 설정 변경 알림 |
| `/topic/room/{roomId}/status` | 방 상태 변경 알림 |

### 게임 진행 채널
| 채널 패턴 | 설명 |
|----------|------|
| `/topic/game/{gameId}/timer` | 게임 타이머 |
| `/topic/game/{gameId}/player` | 플레이어 상태 |
| `/topic/game/{gameId}/round/result` | 라운드 결과 |
| `/topic/game/{gameId}/round/start` | 라운드 시작 |
| `/topic/game/{gameId}/game/finished` | 게임 종료 |

---

## 🔄 메시지 전송 패턴

### 구독 (Subscribe)
클라이언트가 서버로부터 메시지를 받기 위해 사용합니다.

```javascript
stompClient.subscribe('/topic/lobby', function(message) {
  const data = JSON.parse(message.body);
  console.log('수신:', data);
});
```

### 전송 (Send)
클라이언트가 서버로 메시지를 보낼 때 사용합니다.

```javascript
stompClient.send('/app/chat.message.lobby', {}, JSON.stringify({
  content: '안녕하세요!'
}));
```

---

## 📨 공통 응답 형식

### 채팅 메시지

#### 로비 채팅
```json
{
  "senderId": 123,
  "messageId": "uuid",
  "nickname": "홍길동",
  "content": "안녕하세요!",
  "messageType": "GLOBAL_CHAT",
  "timestamp": "2025-11-04T10:30:00"
}
```

#### 게임방 채팅
```json
{
  "senderId": 123,
  "messageId": "uuid",
  "nickname": "홍길동",
  "content": "준비 완료!",
  "messageType": "GAME_ROOM_CHAT",
  "teamId": "TEAM_A",
  "timestamp": "2025-11-04T10:30:00"
}
```

### 게임방 알림

```json
{
  "type": "PLAYER_JOINED",
  "roomId": "123",
  "playerInfo": {
    "memberId": 456,
    "nickname": "김철수",
    "markerImageUrl": "https://example.com/marker.png",
    "team": null,
    "isHost": false,
    "joinedAt": 1730707800000
  },
  "players": null,
  "timestamp": 1730707800000
}
```

---

## 🎯 메시지 타입

### 채팅 메시지 타입
| 타입 | 설명 |
|------|------|
| `GLOBAL_CHAT` | 글로벌 로비 채팅 |
| `GAME_ROOM_CHAT` | 게임방 대기실 채팅 |
| `GAME_CHAT` | 게임 진행 중 전체 채팅 |
| `TEAM_CHAT` | 팀 채팅 (팀전) |
| `SYSTEM_CHAT` | 시스템 메시지 |
| `NOTICE_CHAT` | 공지사항 |

### 알림 타입
| 타입 | 설명 |
|------|------|
| `PLAYER_JOINED` | 플레이어 입장 |
| `PLAYER_LEFT` | 플레이어 퇴장 |
| `PLAYER_KICKED` | 플레이어 강퇴 |
| `PLAYER_LIST_UPDATED` | 플레이어 목록 갱신 |
| `ROOM_SETTINGS_CHANGED` | 방 설정 변경 |
| `GAME_STARTED` | 게임 시작 |

---

## ⚙️ Rate Limiting

모든 채팅 메시지 전송에는 Rate Limiting이 적용됩니다:
- **제한**: 1분당 40개 메시지
- **초과 시**: 추가 메시지 무시

---

## 🔒 보안

### 인증
- WebSocket 연결 시 JWT 토큰 필수
- 토큰 만료 시 연결 종료
- 재연결 시 새 토큰 필요

### 권한
- 방장만 가능: 강퇴, 방 설정 변경
- 팀원만 가능: 팀 채팅 읽기 (팀전)

---

## 🛠️ 에러 처리

### 일반적인 에러
1. **인증 실패**: JWT 토큰이 없거나 만료됨
2. **권한 부족**: 권한이 없는 작업 시도
3. **Rate Limit 초과**: 메시지 전송 제한 초과
4. **연결 끊김**: 네트워크 오류

### 권장 처리 방법
```javascript
stompClient.connect(headers, 
  function onConnect(frame) {
    // 연결 성공
    console.log('Connected');
  },
  function onError(error) {
    // 에러 처리
    console.error('Connection error:', error);
    
    // 재연결 로직
    setTimeout(reconnect, 5000);
  }
);
```

---

## 📚 관련 문서

- [REST API 문서](../README.md)
- [멀티 게임 API](../multi/README.md)
- [Redis 데이터 구조](../../../Redis-Data-Structure-Guide.md)
- [Redis-STOMP 통합](../../../Redis-STOMP-Integration-Guide.md)

---

## 💡 사용 예시

### 로비 채팅 기본 예시
```javascript
// 연결
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
  'Authorization': 'Bearer ' + token
}, function(frame) {
  // 로비 입장
  stompClient.send('/app/chat.join.lobby', {}, '{}');
  
  // 메시지 구독
  stompClient.subscribe('/topic/lobby', function(message) {
    const data = JSON.parse(message.body);
    console.log(`[${data.nickname}]: ${data.content}`);
  });
  
  // 메시지 전송
  stompClient.send('/app/chat.message.lobby', {}, JSON.stringify({
    content: '안녕하세요!'
  }));
});
```

### 게임방 플레이어 목록 추적 예시
```javascript
const roomId = 123;

stompClient.subscribe(`/topic/room/${roomId}/playerList`, function(message) {
  const notification = JSON.parse(message.body);
  
  switch(notification.type) {
    case 'PLAYER_JOINED':
      addPlayerToUI(notification.playerInfo);
      break;
    case 'PLAYER_LEFT':
      removePlayerFromUI(notification.playerInfo.memberId);
      break;
    case 'PLAYER_LIST_UPDATED':
      updateFullPlayerList(notification.players);
      break;
  }
});
```

---

## 🔍 주요 특징

### 1. 실시간 동기화
- 10초마다 자동으로 플레이어 목록 동기화
- 네트워크 오류 시 자동 복구

### 2. 중복 메시지 방지
- UUID 기반 메시지 ID
- 서버에서 5분간 중복 감지

### 3. 팀 지원
- 개인전/팀전 모드 지원
- 팀별 채팅 분리
- 실시간 팀 변경

### 4. 확장 가능한 구조
- 채널 기반 아키텍처
- 새로운 알림 타입 추가 용이
- 다양한 게임 모드 지원 가능

---

## ⚠️ 주의사항

1. **구독 순서**: 메시지를 전송하기 전에 먼저 채널을 구독하세요.
2. **연결 해제**: 페이지 이탈 시 반드시 연결을 해제하세요.
3. **토큰 관리**: JWT 토큰이 만료되기 전에 갱신하세요.
4. **메모리 관리**: 사용하지 않는 구독은 해제하세요.
5. **에러 처리**: 재연결 로직을 반드시 구현하세요.

---

## 📞 문의

WebSocket 관련 문제가 있으면 백엔드 팀에 문의해주세요.

