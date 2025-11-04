# 게임방 WebSocket API 명세서

게임방 채팅, 플레이어 목록 관리 및 실시간 알림 관련 WebSocket API 문서입니다.

---

## 📋 목차

- [연결 설정](#연결-설정)
- [게임방 채팅](#게임방-채팅)
- [플레이어 목록 관리](#플레이어-목록-관리)
- [팀 변경](#팀-변경)
- [실시간 알림](#실시간-알림)
- [알림 타입](#알림-타입)
- [에러 처리](#에러-처리)

---

## 연결 설정

### WebSocket 엔드포인트
```
ws://localhost:8080/ws
```

### 게임방 관련 채널 패턴
```
/topic/room/{roomId}/chat          # 채팅
/topic/room/{roomId}/playerList    # 플레이어 목록
/topic/room/{roomId}/settings      # 방 설정 변경
/topic/room/{roomId}/status        # 방 상태 변경
```

**참고사항**
- `{roomId}`는 게임방 ID로 대체됩니다. (예: `/topic/room/123/chat`)
- WebSocket 연결 시 `Authorization` 헤더에 JWT 토큰이 필수입니다.
- STOMP 프로토콜을 사용합니다.

---

## 게임방 채팅

### 1. 채팅 메시지 전송

**전송 경로**
```
/app/room.{roomId}.chat
```

**구독 경로 (수신용)**
```
/topic/room/{roomId}/chat
```

**전송 메시지 형식**
```json
{
  "content": "준비 완료!",
  "team": "TEAM_A"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| content | String | O | 채팅 메시지 내용 (비어있으면 안됨) |
| team | String | X | 팀 ID (팀전인 경우 팀 채팅용) |

**전송 예시 (JavaScript)**
```javascript
const roomId = 123;

// 1. 구독 설정
stompClient.subscribe(`/topic/room/${roomId}/chat`, function(message) {
  const data = JSON.parse(message.body);
  console.log('받은 메시지:', data);
});

// 2. 일반 채팅 전송
stompClient.send(`/app/room.${roomId}.chat`, {}, JSON.stringify({
  content: '준비 완료!'
}));

// 3. 팀 채팅 전송 (팀전인 경우)
stompClient.send(`/app/room.${roomId}.chat`, {}, JSON.stringify({
  content: '여기로 마커 찍자!',
  team: 'TEAM_A'
}));
```

---

### 2. 채팅 메시지 수신

**구독 경로**
```
/topic/room/{roomId}/chat
```

**수신 메시지 형식**
```json
{
  "senderId": 123,
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "nickname": "홍길동",
  "content": "준비 완료!",
  "messageType": "GAME_ROOM_CHAT",
  "teamId": "TEAM_A",
  "timestamp": "2025-11-04T10:30:00"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| senderId | Long | 메시지 발신자의 회원 ID |
| messageId | String | 메시지 고유 ID (UUID) - 중복 메시지 방지용 |
| nickname | String | 발신자 닉네임 |
| content | String | 채팅 메시지 내용 |
| messageType | String | 메시지 타입 ("GAME_ROOM_CHAT", "TEAM_CHAT" 등) |
| teamId | String | 팀 ID (팀 채팅인 경우에만, null 가능) |
| timestamp | DateTime | 메시지 전송 시각 (ISO 8601 형식) |

---

## 플레이어 목록 관리

### 1. 플레이어 목록 구독

**구독 경로**
```
/topic/room/{roomId}/playerList
```

**구독 시점**
- 게임방에 입장했을 때
- 플레이어 목록 변경 사항을 실시간으로 받고 싶을 때

**구독 예시 (JavaScript)**
```javascript
const roomId = 123;

stompClient.subscribe(`/topic/room/${roomId}/playerList`, function(message) {
  const notification = JSON.parse(message.body);
  console.log('알림 타입:', notification.type);
  console.log('플레이어 목록:', notification.players);
  
  // 알림 타입에 따라 처리
  switch(notification.type) {
    case 'PLAYER_JOINED':
      console.log('플레이어 입장:', notification.playerInfo);
      break;
    case 'PLAYER_LEFT':
      console.log('플레이어 퇴장:', notification.playerInfo);
      break;
    case 'PLAYER_KICKED':
      console.log('플레이어 강퇴:', notification.playerInfo);
      break;
    case 'PLAYER_LIST_UPDATED':
      console.log('플레이어 목록 갱신:', notification.players);
      updatePlayerList(notification.players);
      break;
    case 'GAME_STARTED':
      console.log('게임 시작!');
      break;
  }
});
```

---

### 2. 플레이어 목록 업데이트 수신

게임방에서는 다음과 같은 경우에 플레이어 목록 업데이트가 전송됩니다:

1. **플레이어 입장** (REST API `/rooms/{roomId}/players` POST 호출 시)
2. **플레이어 퇴장** (REST API `/rooms/{roomId}/players` DELETE 호출 시)
3. **플레이어 강퇴** (REST API `/rooms/{roomId}/players/kick` DELETE 호출 시)
4. **주기적 브로드캐스트** (10초마다 자동)
5. **팀 변경** (WebSocket `/app/room.{roomId}.switchTeam` 호출 시)

---

#### 2-1. 플레이어 입장 알림

**수신 메시지 형식**
```json
{
  "type": "PLAYER_JOINED",
  "roomId": "123",
  "playerInfo": {
    "memberId": 456,
    "nickname": "김철수",
    "markerImageUrl": "https://example.com/marker/456.png",
    "team": null,
    "isHost": false,
    "joinedAt": 1730707800000
  },
  "players": null,
  "timestamp": 1730707800000
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| type | String | 알림 타입 ("PLAYER_JOINED") |
| roomId | String | 게임방 ID |
| playerInfo | Object | 입장한 플레이어 정보 |
| players | Array | null (개별 이벤트에서는 null) |
| timestamp | Long | 알림 발생 시각 (밀리초) |

**playerInfo 필드 상세**
| 필드 | 타입 | 설명 |
|------|------|------|
| memberId | Long | 회원 ID |
| nickname | String | 플레이어 닉네임 |
| markerImageUrl | String | 플레이어 마커 이미지 URL |
| team | String | 팀 ID (개인전: null, 팀전: "TEAM_A" 또는 "TEAM_B") |
| isHost | Boolean | 방장 여부 |
| joinedAt | Long | 입장 시각 (밀리초) |

---

#### 2-2. 플레이어 퇴장 알림

**수신 메시지 형식**
```json
{
  "type": "PLAYER_LEFT",
  "roomId": "123",
  "playerInfo": {
    "memberId": 456,
    "nickname": "김철수",
    "markerImageUrl": "https://example.com/marker/456.png",
    "team": null,
    "isHost": false,
    "joinedAt": 1730707800000
  },
  "players": null,
  "timestamp": 1730708000000
}
```

**필드 설명**
- `type`: "PLAYER_LEFT"
- 나머지 필드는 [플레이어 입장 알림](#2-1-플레이어-입장-알림)과 동일

---

#### 2-3. 플레이어 강퇴 알림

**수신 메시지 형식**
```json
{
  "type": "PLAYER_KICKED",
  "roomId": "123",
  "playerInfo": {
    "memberId": 456,
    "nickname": "김철수",
    "markerImageUrl": "https://example.com/marker/456.png",
    "team": null,
    "isHost": false,
    "joinedAt": 1730707800000
  },
  "players": null,
  "timestamp": 1730708000000
}
```

**필드 설명**
- `type`: "PLAYER_KICKED"
- 나머지 필드는 [플레이어 입장 알림](#2-1-플레이어-입장-알림)과 동일

---

#### 2-4. 플레이어 목록 전체 갱신

이 알림은 다음 경우에 발생합니다:
- **주기적 브로드캐스트**: 10초마다 자동으로 전송
- **팀 변경 시**: 플레이어가 팀을 변경했을 때

**수신 메시지 형식**
```json
{
  "type": "PLAYER_LIST_UPDATED",
  "roomId": "123",
  "playerInfo": null,
  "players": [
    {
      "memberId": 123,
      "nickname": "홍길동",
      "markerImageUrl": "https://example.com/marker/123.png",
      "team": "TEAM_A",
      "isHost": true,
      "joinedAt": 1730707600000
    },
    {
      "memberId": 456,
      "nickname": "김철수",
      "markerImageUrl": "https://example.com/marker/456.png",
      "team": "TEAM_B",
      "isHost": false,
      "joinedAt": 1730707800000
    }
  ],
  "timestamp": 1730708000000
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| type | String | 알림 타입 ("PLAYER_LIST_UPDATED") |
| roomId | String | 게임방 ID |
| playerInfo | Object | null (전체 갱신에서는 null) |
| players | Array | 현재 게임방의 모든 플레이어 목록 |
| timestamp | Long | 알림 발생 시각 (밀리초) |

**처리 예시**
```javascript
stompClient.subscribe(`/topic/room/${roomId}/playerList`, function(message) {
  const notification = JSON.parse(message.body);
  
  if (notification.type === 'PLAYER_LIST_UPDATED') {
    // 플레이어 목록 전체를 새로 렌더링
    updatePlayerListUI(notification.players);
  }
});

function updatePlayerListUI(players) {
  const listElement = document.getElementById('player-list');
  listElement.innerHTML = '';
  
  players.forEach(player => {
    const playerElement = document.createElement('div');
    playerElement.innerHTML = `
      <img src="${player.markerImageUrl}" alt="marker">
      <span>${player.nickname}</span>
      ${player.isHost ? '<span class="host-badge">방장</span>' : ''}
      ${player.team ? `<span class="team-badge">${player.team}</span>` : ''}
    `;
    listElement.appendChild(playerElement);
  });
}
```

---

#### 2-5. 게임 시작 알림

**수신 메시지 형식**
```json
{
  "type": "GAME_STARTED",
  "roomId": "123",
  "playerInfo": null,
  "players": null,
  "timestamp": 1730708000000
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| type | String | 알림 타입 ("GAME_STARTED") |
| roomId | String | 게임방 ID |
| timestamp | Long | 게임 시작 시각 (밀리초) |

---

## 팀 변경

팀전 모드에서 플레이어가 팀을 변경할 수 있습니다.

### 팀 변경 요청

**전송 경로**
```
/app/room.{roomId}.switchTeam
```

**전송 메시지 형식**
```json
{
  "team": "TEAM_B"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| team | String | O | 변경할 팀 ID ("TEAM_A" 또는 "TEAM_B") |

**전송 예시 (JavaScript)**
```javascript
const roomId = 123;

// TEAM_B로 팀 변경
stompClient.send(`/app/room.${roomId}.switchTeam`, {}, JSON.stringify({
  team: 'TEAM_B'
}));
```

**응답**
- 팀 변경 후 자동으로 [플레이어 목록 전체 갱신](#2-4-플레이어-목록-전체-갱신) 알림이 발생합니다.
- `/topic/room/{roomId}/playerList`를 구독하고 있으면 업데이트를 받을 수 있습니다.

**참고사항**
- 팀전 모드에서만 사용 가능합니다.
- 개인전 모드에서는 무시됩니다.

---

## 실시간 알림

### 주기적 브로드캐스트

게임방에 있는 동안 **10초마다** 자동으로 플레이어 목록 전체 갱신 알림이 전송됩니다.

**목적**
- 네트워크 오류로 인해 누락된 알림 복구
- 플레이어 상태 동기화
- Redis와 클라이언트 간 데이터 일관성 유지

**수신 형식**
- [플레이어 목록 전체 갱신](#2-4-플레이어-목록-전체-갱신)과 동일

**처리 권장사항**
```javascript
// 마지막 업데이트 시각 추적
let lastUpdate = Date.now();

stompClient.subscribe(`/topic/room/${roomId}/playerList`, function(message) {
  const notification = JSON.parse(message.body);
  
  if (notification.type === 'PLAYER_LIST_UPDATED') {
    const now = Date.now();
    const timeSinceLastUpdate = now - lastUpdate;
    
    // 10초 이상 차이나면 주기적 브로드캐스트
    if (timeSinceLastUpdate >= 10000) {
      console.log('주기적 동기화:', notification.players);
    } else {
      console.log('즉시 업데이트:', notification.players);
    }
    
    updatePlayerListUI(notification.players);
    lastUpdate = now;
  }
});
```

---

## 알림 타입

게임방에서 사용되는 알림 타입은 다음과 같습니다:

| 타입 | 값 | 설명 | 발생 시점 |
|------|------|------|----------|
| 플레이어 입장 | `PLAYER_JOINED` | 새 플레이어가 게임방에 입장 | REST API 방 참여 시 |
| 플레이어 퇴장 | `PLAYER_LEFT` | 플레이어가 게임방에서 퇴장 | REST API 방 퇴장 시 |
| 플레이어 강퇴 | `PLAYER_KICKED` | 플레이어가 방장에 의해 강퇴됨 | REST API 강퇴 시 |
| 플레이어 목록 갱신 | `PLAYER_LIST_UPDATED` | 전체 플레이어 목록 동기화 | 10초마다 / 팀 변경 시 |
| 방 설정 변경 | `ROOM_SETTINGS_CHANGED` | 게임방 설정이 변경됨 | 방 설정 수정 시 |
| 게임 시작 | `GAME_STARTED` | 게임이 시작됨 | 게임 시작 시 |

---

## 메시지 타입

게임방 채팅에서 사용되는 `messageType`은 다음과 같습니다:

| 타입 | 값 | 설명 |
|------|------|------|
| 게임방 채팅 | `GAME_ROOM_CHAT` | 게임방 대기실 채팅 |
| 게임 내 채팅 | `GAME_CHAT` | 게임 진행 중 전체 채팅 |
| 팀 채팅 | `TEAM_CHAT` | 팀원들만 보는 채팅 (팀전) |
| 시스템 메시지 | `SYSTEM_CHAT` | 시스템 알림 |

---

## 에러 처리

### Rate Limiting
- 1분에 최대 40개의 메시지 전송 가능
- 제한 초과 시 추가 메시지는 무시됩니다.

### 권한 에러
- 방장만 가능한 작업 (강퇴, 설정 변경)을 일반 플레이어가 시도하면 에러 발생
- 게임방에 참여하지 않은 상태에서 채팅 전송 시 무시됩니다.

### 연결 끊김 처리
```javascript
// 재연결 로직
let reconnectAttempts = 0;
const maxReconnectAttempts = 5;

function connect() {
  const socket = new SockJS('http://localhost:8080/ws');
  const stompClient = Stomp.over(socket);
  
  stompClient.connect({
    'Authorization': 'Bearer ' + accessToken
  }, function(frame) {
    console.log('Connected');
    reconnectAttempts = 0;
    
    // 구독 재설정
    subscribeToGameRoom(roomId);
    
  }, function(error) {
    console.error('Connection error:', error);
    
    if (reconnectAttempts < maxReconnectAttempts) {
      reconnectAttempts++;
      setTimeout(connect, 5000); // 5초 후 재연결
    }
  });
}
```

---

## 전체 사용 예시 (JavaScript)

```javascript
const roomId = 123;
let stompClient = null;

// WebSocket 연결
function connectToGameRoom(roomId) {
  const socket = new SockJS('http://localhost:8080/ws');
  stompClient = Stomp.over(socket);
  
  stompClient.connect({
    'Authorization': 'Bearer ' + accessToken
  }, function(frame) {
    console.log('Connected to game room:', roomId);
    
    // 1. 플레이어 목록 구독
    stompClient.subscribe(`/topic/room/${roomId}/playerList`, function(message) {
      const notification = JSON.parse(message.body);
      handlePlayerNotification(notification);
    });
    
    // 2. 채팅 구독
    stompClient.subscribe(`/topic/room/${roomId}/chat`, function(message) {
      const chatMessage = JSON.parse(message.body);
      displayChatMessage(chatMessage);
    });
  });
}

// 플레이어 알림 처리
function handlePlayerNotification(notification) {
  switch(notification.type) {
    case 'PLAYER_JOINED':
      console.log(`${notification.playerInfo.nickname}님이 입장했습니다.`);
      addPlayerToList(notification.playerInfo);
      break;
      
    case 'PLAYER_LEFT':
      console.log(`${notification.playerInfo.nickname}님이 퇴장했습니다.`);
      removePlayerFromList(notification.playerInfo.memberId);
      break;
      
    case 'PLAYER_KICKED':
      console.log(`${notification.playerInfo.nickname}님이 강퇴되었습니다.`);
      removePlayerFromList(notification.playerInfo.memberId);
      break;
      
    case 'PLAYER_LIST_UPDATED':
      console.log('플레이어 목록 갱신');
      updatePlayerListUI(notification.players);
      break;
      
    case 'GAME_STARTED':
      console.log('게임이 시작되었습니다!');
      redirectToGame();
      break;
  }
}

// 채팅 메시지 전송
function sendChatMessage(content) {
  stompClient.send(`/app/room.${roomId}.chat`, {}, JSON.stringify({
    content: content
  }));
}

// 팀 변경
function switchTeam(teamId) {
  stompClient.send(`/app/room.${roomId}.switchTeam`, {}, JSON.stringify({
    team: teamId
  }));
}

// 채팅 메시지 표시
function displayChatMessage(message) {
  const chatBox = document.getElementById('chat-box');
  const messageElement = document.createElement('div');
  messageElement.innerHTML = `
    <span class="nickname">${message.nickname}</span>: 
    <span class="content">${message.content}</span>
    ${message.teamId ? `<span class="team">[${message.teamId}]</span>` : ''}
  `;
  chatBox.appendChild(messageElement);
  chatBox.scrollTop = chatBox.scrollHeight;
}

// 플레이어 목록 UI 업데이트
function updatePlayerListUI(players) {
  const playerList = document.getElementById('player-list');
  playerList.innerHTML = '';
  
  players.forEach(player => {
    const playerElement = document.createElement('div');
    playerElement.className = 'player-item';
    playerElement.innerHTML = `
      <img src="${player.markerImageUrl}" alt="marker" class="player-marker">
      <span class="player-nickname">${player.nickname}</span>
      ${player.isHost ? '<span class="host-badge">방장</span>' : ''}
      ${player.team ? `<span class="team-badge">${player.team}</span>` : ''}
    `;
    playerList.appendChild(playerElement);
  });
}

// 연결 해제
function disconnectFromGameRoom() {
  if (stompClient !== null) {
    stompClient.disconnect();
    console.log('Disconnected from game room');
  }
}

// 페이지 이탈 시 연결 해제
window.addEventListener('beforeunload', function() {
  disconnectFromGameRoom();
});

// 사용
connectToGameRoom(123);
```

---

## 주의사항

1. **인증 필수**: WebSocket 연결 시 반드시 유효한 JWT 토큰이 필요합니다.
2. **구독 우선**: 메시지를 전송하기 전에 먼저 채널을 구독해야 응답을 받을 수 있습니다.
3. **주기적 동기화**: 10초마다 자동으로 플레이어 목록이 전송되므로 UI를 적절히 업데이트해야 합니다.
4. **팀 모드 확인**: 팀 변경 기능은 팀전 모드에서만 사용 가능합니다.
5. **알림 타입 처리**: 각 알림 타입에 따라 적절한 UI 업데이트를 수행해야 합니다.
6. **재연결 처리**: 네트워크 오류 시 재연결 로직을 구현하는 것을 권장합니다.
7. **메모리 관리**: 게임방을 나갈 때는 반드시 구독을 해제하고 연결을 종료해야 합니다.

