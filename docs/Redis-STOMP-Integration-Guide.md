# KoSpot Redis & STOMP 통합 개발 가이드

## 📋 목차
1. [Redis 키 패턴 및 데이터 구조](#redis-키-패턴-및-데이터-구조)
2. [STOMP 채널 상수 및 메시징 패턴](#stomp-채널-상수-및-메시징-패턴)
3. [실제 데이터 저장 예시](#실제-데이터-저장-예시)
4. [프론트엔드 통합 가이드](#프론트엔드-통합-가이드)
5. [개발 시 주의사항](#개발-시-주의사항)

---

## 🔑 Redis 키 패턴 및 데이터 구조

### 📊 게임방 관련 Redis 키 패턴

| 키 패턴 | 설명 | 데이터 타입 | TTL | 예시 |
|---------|------|-------------|-----|------|
| `game:room:{roomId}:players` | 게임방 플레이어 정보 | Hash | 12시간 | `game:room:123:players` |
| `game:room:{roomId}:banned` | 게임방 강퇴 목록 | Set | 12시간 | `game:room:123:banned` |
| `game:player:{memberId}:session` | 플레이어 세션 매핑 | String | 24시간 | `game:player:456:session` |
| `game:session:{sessionId}:subscriptions` | 세션 구독 정보 | Set | 24시간 | `game:session:abc123:subscriptions` |
| `game:session:{sessionId}:room` | 세션-룸 매핑 | String | 24시간 | `game:session:abc123:room` |

### 🎮 게임 타이머 관련 Redis 키 패턴

| 키 패턴 | 설명 | 데이터 타입 | TTL | 예시 |
|---------|------|-------------|-----|------|
| `game:room:{roomId}:round:{roundId}` | 게임 라운드 정보 | Hash | 게임 종료 시 | `game:room:123:round:round1` |
| `game:room:{roomId}:active:rounds` | 활성 라운드 목록 | Set | 게임 종료 시 | `game:room:123:active:rounds` |
| `player:{memberId}:room:{roomId}:round` | 플레이어 라운드 정보 | Hash | 게임 종료 시 | `player:456:room:123:round` |

### 🔧 세션 컨텍스트 Redis 키 패턴

| 키 패턴 | 설명 | 데이터 타입 | TTL | 예시 |
|---------|------|-------------|-----|------|
| `session:ctx:{sessionId}` | 세션 컨텍스트 정보 | Hash | 24시간 | `session:ctx:abc123` |

### 🌐 글로벌 Redis 키 패턴

| 키 패턴 | 설명 | 데이터 타입 | TTL | 예시 |
|---------|------|-------------|-----|------|
| `lobby:users` | 로비 사용자 목록 | Set | 24시간 | `lobby:users` |
| `chat:recent:{chatId}` | 최근 채팅 메시지 | List | 7일 | `chat:recent:global` |

---

## 📡 STOMP 채널 상수 및 메시징 패턴

### 🎯 기본 채널 프리픽스

```java
// 기본 프리픽스
public static final String PREFIX_TOPIC = "/topic/";      // 다대다 브로드캐스트
public static final String PREFIX_USER = "/user/";        // 일대일 개인 메시지
public static final String PREFIX_APP = "/app/";           // 클라이언트 메시지 전송
```

### 🏠 게임방 채널 상수

| 채널 타입 | 패턴 | 설명 | 예시 |
|-----------|------|------|------|
| 플레이어 목록 | `/topic/room/{roomId}/playerList` | 플레이어 입장/퇴장/팀 변경 | `/topic/room/123/playerList` |
| 채팅 | `/topic/room/{roomId}/chat` | 게임방 채팅 메시지 | `/topic/room/123/chat` |
| 설정 | `/topic/room/{roomId}/settings` | 방 설정 변경 알림 | `/topic/room/123/settings` |
| 상태 | `/topic/room/{roomId}/status` | 게임 상태 변경 | `/topic/room/123/status` |

### 🎮 게임 내부 채널 상수

| 채널 타입 | 패턴 | 설명 | 예시 |
|-----------|------|------|------|
| 타이머 | `/topic/game/{roomId}/timer` | 게임 타이머 정보 | `/topic/game/123/timer` |
| 플레이어 상태 | `/topic/game/{roomId}/player` | 플레이어 게임 상태 | `/topic/game/123/player` |
| 로드뷰 제출 | `/topic/game/{roomId}/roadview/submit` | 로드뷰 답안 제출 | `/topic/game/123/roadview/submit` |
| 팀 마커 | `/topic/game/{roomId}/roadview/team/{teamId}/marker` | 팀별 마커 정보 | `/topic/game/123/roadview/team/RED/marker` |
| 포토 제출 | `/topic/game/{roomId}/photo/submit` | 포토게임 답안 제출 | `/topic/game/123/photo/submit` |
| 포토 답안 | `/topic/game/{roomId}/photo/answer` | 포토게임 답안 공개 | `/topic/game/123/photo/answer` |

### 📨 메시지 매핑 패턴

| 매핑 패턴 | 설명 | 예시 |
|-----------|------|------|
| `/app/room.{roomId}.chat` | 게임방 채팅 메시지 전송 | `/app/room.123.chat` |
| `/app/room.{roomId}.switchTeam` | 팀 변경 요청 | `/app/room.123.switchTeam` |

### 🔔 개인 메시지 채널

| 채널 타입 | 패턴 | 설명 | 예시 |
|-----------|------|------|------|
| 개인 알림 | `/user/{memberId}/notification` | 개인 알림 메시지 | `/user/456/notification` |
| 게임 초대 | `/user/{memberId}/gameInvite` | 게임 초대 메시지 | `/user/456/gameInvite` |

### 🌍 글로벌 채널

| 채널 타입 | 패턴 | 설명 |
|-----------|------|------|
| 로비 채팅 | `/topic/chat/lobby` | 글로벌 로비 채팅 |
| 전역 알림 | `/topic/notification/global` | 시스템 전역 알림 |
| 시스템 점검 | `/topic/notification/maintenance` | 시스템 점검 알림 |

---

## 💾 실제 데이터 저장 예시

### 🎮 게임방 플레이어 정보 저장

**Redis 키**: `game:room:123:players`

```json
{
  "12345": {
    "memberId": 12345,
    "nickname": "Player1",
    "markerImageUrl": "https://example.com/marker1.png",
    "team": "RED",
    "isHost": true,
    "joinedAt": 1703123456789
  },
  "67890": {
    "memberId": 67890,
    "nickname": "Player2",
    "markerImageUrl": "https://example.com/marker2.png",
    "team": "BLUE",
    "isHost": false,
    "joinedAt": 1703123456790
  }
}
```

### 🔄 세션 관리 데이터

**Redis 키**: `game:player:12345:session`
```json
"abc123def456"
```

**Redis 키**: `game:session:abc123def456:subscriptions`
```json
["/topic/room/123/playerList", "/topic/room/123/chat"]
```

**Redis 키**: `game:session:abc123def456:room`
```json
"123"
```

### 🎯 게임 라운드 데이터

**Redis 키**: `game:room:123:round:round1`
```json
{
  "roundId": "round1",
  "gameType": "ROADVIEW",
  "question": "이곳은 어디인가요?",
  "timeLimit": 30,
  "startTime": 1703123456789,
  "status": "ACTIVE"
}
```

---

## 🖥️ 프론트엔드 통합 가이드

### 🔌 WebSocket 연결 설정

```javascript
// WebSocket 연결
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// 연결 설정
stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // 게임방 플레이어 목록 구독
    subscribeToPlayerList();
    
    // 게임방 채팅 구독
    subscribeToChat();
    
    // 게임방 설정 변경 구독
    subscribeToSettings();
});
```

### 📡 채널 구독 예시

```javascript
// 1. 플레이어 목록 구독
function subscribeToPlayerList(roomId) {
    stompClient.subscribe(`/topic/room/${roomId}/playerList`, function(message) {
        const playerList = JSON.parse(message.body);
        updatePlayerList(playerList);
    });
}

// 2. 채팅 메시지 구독
function subscribeToChat(roomId) {
    stompClient.subscribe(`/topic/room/${roomId}/chat`, function(message) {
        const chatMessage = JSON.parse(message.body);
        displayChatMessage(chatMessage);
    });
}

// 3. 방 설정 변경 구독
function subscribeToSettings(roomId) {
    stompClient.subscribe(`/topic/room/${roomId}/settings`, function(message) {
        const settings = JSON.parse(message.body);
        updateRoomSettings(settings);
    });
}
```

### 📤 메시지 전송 예시

```javascript
// 1. 채팅 메시지 전송
function sendChatMessage(roomId, message) {
    const chatMessage = {
        content: message,
        timestamp: Date.now()
    };
    
    stompClient.send(`/app/room.${roomId}.chat`, {}, JSON.stringify(chatMessage));
}

// 2. 팀 변경 요청
function switchTeam(roomId, team) {
    const switchRequest = {
        team: team
    };
    
    stompClient.send(`/app/room.${roomId}.switchTeam`, {}, JSON.stringify(switchRequest));
}
```

### 📊 메시지 타입별 처리

```javascript
// 플레이어 목록 업데이트 메시지 처리
function handlePlayerListMessage(message) {
    const data = JSON.parse(message.body);
    
    switch(data.type) {
        case 'PLAYER_JOINED':
            addPlayerToList(data.player);
            showNotification(`${data.player.nickname}님이 입장했습니다.`);
            break;
            
        case 'PLAYER_LEFT':
            removePlayerFromList(data.player.memberId);
            showNotification(`${data.player.nickname}님이 퇴장했습니다.`);
            break;
            
        case 'PLAYER_LIST_UPDATED':
            updateEntirePlayerList(data.players);
            break;
            
        case 'PLAYER_KICKED':
            removePlayerFromList(data.player.memberId);
            showNotification(`${data.player.nickname}님이 강퇴되었습니다.`);
            break;
    }
}

// 방 설정 변경 메시지 처리
function handleSettingsMessage(message) {
    const settings = JSON.parse(message.body);
    
    // 방 제목 업데이트
    if (settings.title) {
        document.getElementById('room-title').textContent = settings.title;
    }
    
    // 게임 모드 업데이트
    if (settings.gameModeKey) {
        updateGameModeDisplay(settings.gameModeKey);
    }
    
    // 팀 모드 변경
    if (settings.playerMatchTypeKey) {
        updateTeamModeDisplay(settings.playerMatchTypeKey);
    }
}
```

### 🔄 실시간 상태 동기화

```javascript
// Redis 데이터와 동기화된 상태 관리
class GameRoomState {
    constructor(roomId) {
        this.roomId = roomId;
        this.players = new Map();
        this.settings = {};
        this.isConnected = false;
    }
    
    // 플레이어 상태 업데이트
    updatePlayer(playerInfo) {
        this.players.set(playerInfo.memberId, playerInfo);
        this.renderPlayerList();
    }
    
    // 플레이어 제거
    removePlayer(memberId) {
        this.players.delete(memberId);
        this.renderPlayerList();
    }
    
    // 전체 플레이어 목록 업데이트
    updatePlayerList(players) {
        this.players.clear();
        players.forEach(player => {
            this.players.set(player.memberId, player);
        });
        this.renderPlayerList();
    }
    
    // UI 렌더링
    renderPlayerList() {
        const playerListElement = document.getElementById('player-list');
        playerListElement.innerHTML = '';
        
        this.players.forEach(player => {
            const playerElement = this.createPlayerElement(player);
            playerListElement.appendChild(playerElement);
        });
    }
}
```

---

## ⚠️ 개발 시 주의사항

### 🔒 Redis 관련 주의사항

1. **키 네이밍 규칙**
   - 모든 키는 소문자와 콜론(:)만 사용
   - 의미있는 네임스페이스 구분
   - 버전 관리 가능한 구조

2. **TTL 설정**
   - 세션 데이터: 24시간
   - 게임방 데이터: 12시간
   - 채팅 데이터: 7일

3. **데이터 직렬화**
   - JSON 형태로 저장
   - Jackson ObjectMapper 사용
   - 예외 처리 필수

### 📡 STOMP 관련 주의사항

1. **채널 네이밍**
   - 소문자와 슬래시(/)만 사용
   - 특수문자 금지
   - 의미있는 계층 구조

2. **메시지 크기**
   - 큰 데이터는 HTTP API 사용
   - Redis는 상태 정보만 저장
   - 실시간 동기화에 집중

3. **연결 관리**
   - 자동 재연결 구현
   - 연결 상태 모니터링
   - 에러 핸들링 필수

### 🚀 성능 최적화 팁

1. **Redis 최적화**
   - Hash 구조 활용
   - 배치 작업 사용
   - 불필요한 데이터 정리

2. **STOMP 최적화**
   - 필요한 채널만 구독
   - 메시지 필터링
   - 브로드캐스트 최소화

3. **프론트엔드 최적화**
   - 상태 캐싱
   - 불필요한 리렌더링 방지
   - 메모리 누수 방지

---

## 📚 참고 자료

- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [Redis Data Types](https://redis.io/docs/data-types/)
- [STOMP Protocol Specification](https://stomp.github.io/stomp-specification-1.2.html)

---

**📝 문서 버전**: 1.0  
**🔄 최종 업데이트**: 2024-01-01  
**👥 작성자**: KoSpot Backend Team
