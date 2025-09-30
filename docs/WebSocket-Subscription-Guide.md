# KoSpot WebSocket 구독 가이드 (프론트엔드용)

## 📋 목차
1. [WebSocket 연결 설정](#websocket-연결-설정)
2. [구독 가능한 채널 목록](#구독-가능한-채널-목록)
3. [메시지 전송 패턴](#메시지-전송-패턴)
4. [메시지 타입별 처리 방법](#메시지-타입별-처리-방법)
5. [실제 사용 예시](#실제-사용-예시)

---

## 🔌 WebSocket 연결 설정

### 기본 연결 설정

```javascript
// WebSocket 연결
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// 연결 옵션
const connectOptions = {
    // 인증 헤더 (필요시)
    // Authorization: 'Bearer ' + token
};

// 연결
stompClient.connect(connectOptions, function(frame) {
    console.log('WebSocket Connected: ' + frame);
    initializeSubscriptions();
}, function(error) {
    console.error('WebSocket Connection Error:', error);
});
```

### 전역 알림 연결 (선택사항)

```javascript
// 전역 알림용 별도 연결
const notificationSocket = new SockJS('/ws/notification');
const notificationClient = Stomp.over(notificationSocket);

notificationClient.connect({}, function(frame) {
    console.log('Notification WebSocket Connected: ' + frame);
    subscribeToGlobalNotifications();
});
```

---

## 📡 구독 가능한 채널 목록

### 🏠 게임방 채널

#### 1. 플레이어 목록 채널
```javascript
// 채널: /topic/room/{roomId}/playerList
// 설명: 플레이어 입장/퇴장/팀 변경/강퇴 등 모든 플레이어 관련 이벤트

function subscribeToPlayerList(roomId) {
    return stompClient.subscribe(`/topic/room/${roomId}/playerList`, function(message) {
        const data = JSON.parse(message.body);
        handlePlayerListMessage(data);
    });
}
```

#### 2. 채팅 채널
```javascript
// 채널: /topic/room/{roomId}/chat
// 설명: 게임방 내 채팅 메시지

function subscribeToChat(roomId) {
    return stompClient.subscribe(`/topic/room/${roomId}/chat`, function(message) {
        const chatMessage = JSON.parse(message.body);
        displayChatMessage(chatMessage);
    });
}
```

#### 3. 방 설정 채널
```javascript
// 채널: /topic/room/{roomId}/settings
// 설명: 방 제목, 게임 모드, 팀 설정 등 변경 알림

function subscribeToSettings(roomId) {
    return stompClient.subscribe(`/topic/room/${roomId}/settings`, function(message) {
        const settings = JSON.parse(message.body);
        updateRoomSettings(settings);
    });
}
```

#### 4. 방 상태 채널
```javascript
// 채널: /topic/room/{roomId}/status
// 설명: 게임 시작/종료, 방 상태 변경 등

function subscribeToStatus(roomId) {
    return stompClient.subscribe(`/topic/room/${roomId}/status`, function(message) {
        const status = JSON.parse(message.body);
        handleRoomStatusChange(status);
    });
}
```

### 🎮 게임 내부 채널

#### 1. 게임 타이머 채널
```javascript
// 채널: /topic/game/{roomId}/timer
// 설명: 게임 타이머 정보 (남은 시간, 라운드 정보 등)

function subscribeToGameTimer(roomId) {
    return stompClient.subscribe(`/topic/game/${roomId}/timer`, function(message) {
        const timerInfo = JSON.parse(message.body);
        updateGameTimer(timerInfo);
    });
}
```

#### 2. 플레이어 게임 상태 채널
```javascript
// 채널: /topic/game/{roomId}/player
// 설명: 플레이어의 게임 내 상태 (점수, 답안 제출 등)

function subscribeToPlayerState(roomId) {
    return stompClient.subscribe(`/topic/game/${roomId}/player`, function(message) {
        const playerState = JSON.parse(message.body);
        updatePlayerGameState(playerState);
    });
}
```

#### 3. 로드뷰 게임 채널
```javascript
// 로드뷰 답안 제출 채널
// 채널: /topic/game/{roomId}/roadview/submit
function subscribeToRoadViewSubmit(roomId) {
    return stompClient.subscribe(`/topic/game/${roomId}/roadview/submit`, function(message) {
        const submission = JSON.parse(message.body);
        handleRoadViewSubmission(submission);
    });
}

// 팀별 마커 채널 (팀원만 볼 수 있음)
// 채널: /topic/game/{roomId}/roadview/team/{teamId}/marker
function subscribeToTeamMarker(roomId, teamId) {
    return stompClient.subscribe(`/topic/game/${roomId}/roadview/team/${teamId}/marker`, function(message) {
        const markerInfo = JSON.parse(message.body);
        updateTeamMarker(markerInfo);
    });
}
```

#### 4. 포토게임 채널
```javascript
// 포토 답안 제출 채널
// 채널: /topic/game/{roomId}/photo/submit
function subscribeToPhotoSubmit(roomId) {
    return stompClient.subscribe(`/topic/game/${roomId}/photo/submit`, function(message) {
        const submission = JSON.parse(message.body);
        handlePhotoSubmission(submission);
    });
}

// 포토 답안 공개 채널
// 채널: /topic/game/{roomId}/photo/answer
function subscribeToPhotoAnswer(roomId) {
    return stompClient.subscribe(`/topic/game/${roomId}/photo/answer`, function(message) {
        const answer = JSON.parse(message.body);
        revealPhotoAnswer(answer);
    });
}
```

### 🔔 개인 메시지 채널

#### 1. 개인 알림 채널
```javascript
// 채널: /user/{memberId}/notification
// 설명: 개인에게만 전송되는 알림 (게임 초대, 친구 요청 등)

function subscribeToPersonalNotification(memberId) {
    return stompClient.subscribe(`/user/${memberId}/notification`, function(message) {
        const notification = JSON.parse(message.body);
        showPersonalNotification(notification);
    });
}
```

#### 2. 게임 초대 채널
```javascript
// 채널: /user/{memberId}/gameInvite
// 설명: 게임 초대 메시지

function subscribeToGameInvite(memberId) {
    return stompClient.subscribe(`/user/${memberId}/gameInvite`, function(message) {
        const invite = JSON.parse(message.body);
        handleGameInvite(invite);
    });
}
```

### 🌍 글로벌 채널

#### 1. 로비 채팅 채널
```javascript
// 채널: /topic/chat/lobby
// 설명: 전역 로비 채팅

function subscribeToLobbyChat() {
    return stompClient.subscribe('/topic/chat/lobby', function(message) {
        const chatMessage = JSON.parse(message.body);
        displayLobbyChat(chatMessage);
    });
}
```

#### 2. 전역 알림 채널
```javascript
// 채널: /topic/notification/global
// 설명: 시스템 전역 알림

function subscribeToGlobalNotification() {
    return stompClient.subscribe('/topic/notification/global', function(message) {
        const notification = JSON.parse(message.body);
        showGlobalNotification(notification);
    });
}
```

#### 3. 시스템 점검 채널
```javascript
// 채널: /topic/notification/maintenance
// 설명: 시스템 점검 알림

function subscribeToMaintenanceNotification() {
    return stompClient.subscribe('/topic/notification/maintenance', function(message) {
        const maintenance = JSON.parse(message.body);
        showMaintenanceNotification(maintenance);
    });
}
```

---

## 📤 메시지 전송 패턴

### 게임방 메시지 전송

#### 1. 채팅 메시지 전송
```javascript
// 전송 경로: /app/room.{roomId}.chat
function sendChatMessage(roomId, content) {
    const message = {
        content: content,
        timestamp: Date.now()
    };
    
    stompClient.send(`/app/room.${roomId}.chat`, {}, JSON.stringify(message));
}
```

#### 2. 팀 변경 요청
```javascript
// 전송 경로: /app/room.{roomId}.switchTeam
function switchTeam(roomId, team) {
    const request = {
        team: team  // "RED", "BLUE", null
    };
    
    stompClient.send(`/app/room.${roomId}.switchTeam`, {}, JSON.stringify(request));
}
```

### 게임 내부 메시지 전송

#### 1. 로드뷰 답안 제출
```javascript
// 전송 경로: /app/game.{roomId}.roadview.submit
function submitRoadViewAnswer(roomId, answer) {
    const submission = {
        answer: answer,
        latitude: currentLatitude,
        longitude: currentLongitude,
        timestamp: Date.now()
    };
    
    stompClient.send(`/app/game.${roomId}.roadview.submit`, {}, JSON.stringify(submission));
}
```

#### 2. 포토 답안 제출
```javascript
// 전송 경로: /app/game.{roomId}.photo.submit
function submitPhotoAnswer(roomId, answer) {
    const submission = {
        answer: answer,
        timestamp: Date.now()
    };
    
    stompClient.send(`/app/game.${roomId}.photo.submit`, {}, JSON.stringify(submission));
}
```

---

## 📊 메시지 타입별 처리 방법

### 플레이어 목록 메시지 처리

```javascript
function handlePlayerListMessage(data) {
    switch(data.type) {
        case 'PLAYER_JOINED':
            // 플레이어 입장
            addPlayerToList(data.player);
            showNotification(`${data.player.nickname}님이 입장했습니다.`);
            break;
            
        case 'PLAYER_LEFT':
            // 플레이어 퇴장
            removePlayerFromList(data.player.memberId);
            showNotification(`${data.player.nickname}님이 퇴장했습니다.`);
            break;
            
        case 'PLAYER_LIST_UPDATED':
            // 전체 플레이어 목록 업데이트
            updateEntirePlayerList(data.players);
            break;
            
        case 'PLAYER_KICKED':
            // 플레이어 강퇴
            removePlayerFromList(data.player.memberId);
            showNotification(`${data.player.nickname}님이 강퇴되었습니다.`);
            break;
            
        case 'TEAM_CHANGED':
            // 팀 변경
            updatePlayerTeam(data.player.memberId, data.player.team);
            break;
            
        case 'HOST_CHANGED':
            // 방장 변경
            updateHost(data.newHost.memberId);
            showNotification(`${data.newHost.nickname}님이 새 방장이 되었습니다.`);
            break;
    }
}
```

### 채팅 메시지 처리

```javascript
function displayChatMessage(chatMessage) {
    const messageElement = document.createElement('div');
    messageElement.className = 'chat-message';
    
    messageElement.innerHTML = `
        <div class="message-header">
            <span class="sender">${chatMessage.senderNickname}</span>
            <span class="timestamp">${formatTime(chatMessage.timestamp)}</span>
        </div>
        <div class="message-content">${chatMessage.content}</div>
    `;
    
    document.getElementById('chat-container').appendChild(messageElement);
    scrollToBottom();
}
```

### 방 설정 변경 처리

```javascript
function updateRoomSettings(settings) {
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
        
        // 팀 모드가 변경되면 플레이어 목록 새로고침
        if (settings.playerMatchTypeKey === 'SOLO') {
            clearTeamAssignments();
        } else if (settings.playerMatchTypeKey === 'TEAM') {
            assignTeamsAutomatically();
        }
    }
    
    // 비밀방 설정
    if (settings.privateRoom !== undefined) {
        updatePrivateRoomIndicator(settings.privateRoom);
    }
}
```

---

## 💡 실제 사용 예시

### 게임방 입장 시 구독 설정

```javascript
class GameRoomManager {
    constructor(roomId, memberId) {
        this.roomId = roomId;
        this.memberId = memberId;
        this.subscriptions = new Map();
    }
    
    // 게임방 입장 시 모든 구독 설정
    initializeSubscriptions() {
        // 플레이어 목록 구독
        this.subscriptions.set('playerList', 
            subscribeToPlayerList(this.roomId));
        
        // 채팅 구독
        this.subscriptions.set('chat', 
            subscribeToChat(this.roomId));
        
        // 방 설정 구독
        this.subscriptions.set('settings', 
            subscribeToSettings(this.roomId));
        
        // 방 상태 구독
        this.subscriptions.set('status', 
            subscribeToStatus(this.roomId));
        
        // 개인 알림 구독
        this.subscriptions.set('personalNotification', 
            subscribeToPersonalNotification(this.memberId));
    }
    
    // 게임 시작 시 추가 구독
    startGame() {
        // 게임 타이머 구독
        this.subscriptions.set('timer', 
            subscribeToGameTimer(this.roomId));
        
        // 플레이어 상태 구독
        this.subscriptions.set('playerState', 
            subscribeToPlayerState(this.roomId));
        
        // 게임 타입에 따른 구독
        const gameType = getCurrentGameType();
        if (gameType === 'ROADVIEW') {
            this.subscriptions.set('roadViewSubmit', 
                subscribeToRoadViewSubmit(this.roomId));
            
            // 팀 모드인 경우 팀 마커 구독
            const teamId = getCurrentTeam();
            if (teamId) {
                this.subscriptions.set('teamMarker', 
                    subscribeToTeamMarker(this.roomId, teamId));
            }
        } else if (gameType === 'PHOTO') {
            this.subscriptions.set('photoSubmit', 
                subscribeToPhotoSubmit(this.roomId));
            this.subscriptions.set('photoAnswer', 
                subscribeToPhotoAnswer(this.roomId));
        }
    }
    
    // 게임방 퇴장 시 구독 해제
    leaveRoom() {
        this.subscriptions.forEach((subscription, key) => {
            subscription.unsubscribe();
        });
        this.subscriptions.clear();
    }
}
```

### 에러 처리 및 재연결

```javascript
class WebSocketManager {
    constructor() {
        this.stompClient = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000;
    }
    
    connect() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        // 연결 성공
        this.stompClient.connect({}, (frame) => {
            console.log('Connected: ' + frame);
            this.reconnectAttempts = 0;
            this.onConnected();
        }, (error) => {
            console.error('Connection error:', error);
            this.handleReconnect();
        });
    }
    
    handleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`Reconnecting... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
            
            setTimeout(() => {
                this.connect();
            }, this.reconnectDelay * this.reconnectAttempts);
        } else {
            console.error('Max reconnection attempts reached');
            this.onConnectionFailed();
        }
    }
    
    onConnected() {
        // 연결 성공 시 필요한 구독들 설정
        this.setupDefaultSubscriptions();
    }
    
    onConnectionFailed() {
        // 연결 실패 시 사용자에게 알림
        showErrorNotification('서버 연결에 실패했습니다. 페이지를 새로고침해주세요.');
    }
}
```

---

## 📚 채널 요약표

| 채널 타입 | 채널 패턴 | 설명 | 사용 시점 |
|-----------|-----------|------|-----------|
| **게임방 플레이어** | `/topic/room/{roomId}/playerList` | 플레이어 입장/퇴장/팀 변경 | 게임방 입장 시 |
| **게임방 채팅** | `/topic/room/{roomId}/chat` | 채팅 메시지 | 게임방 입장 시 |
| **게임방 설정** | `/topic/room/{roomId}/settings` | 방 설정 변경 | 게임방 입장 시 |
| **게임방 상태** | `/topic/room/{roomId}/status` | 게임 시작/종료 | 게임방 입장 시 |
| **게임 타이머** | `/topic/game/{roomId}/timer` | 타이머 정보 | 게임 시작 시 |
| **플레이어 상태** | `/topic/game/{roomId}/player` | 플레이어 게임 상태 | 게임 시작 시 |
| **로드뷰 제출** | `/topic/game/{roomId}/roadview/submit` | 로드뷰 답안 제출 | 로드뷰 게임 시 |
| **팀 마커** | `/topic/game/{roomId}/roadview/team/{teamId}/marker` | 팀별 마커 정보 | 팀 로드뷰 게임 시 |
| **포토 제출** | `/topic/game/{roomId}/photo/submit` | 포토 답안 제출 | 포토 게임 시 |
| **포토 답안** | `/topic/game/{roomId}/photo/answer` | 포토 답안 공개 | 포토 게임 시 |
| **개인 알림** | `/user/{memberId}/notification` | 개인 알림 | 로그인 시 |
| **게임 초대** | `/user/{memberId}/gameInvite` | 게임 초대 | 로그인 시 |
| **로비 채팅** | `/topic/chat/lobby` | 전역 채팅 | 로비 입장 시 |
| **전역 알림** | `/topic/notification/global` | 시스템 알림 | 앱 시작 시 |

---

**📝 문서 버전**: 1.0  
**🔄 최종 업데이트**: 2024-01-01  
**👥 작성자**: KoSpot Frontend Team
