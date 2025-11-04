# Multi Game API Documentation

멀티플레이 게임 관련 API 문서입니다. WebSocket 및 REST API가 포함되어 있습니다.

## 📋 목차

- [게임 방 관리](#게임-방-관리)
- [멀티 게임 플레이](#멀티-게임-플레이)
- [게임 정답 제출](#게임-정답-제출)
- [WebSocket API](#websocket-api)

---

## 게임 방 관리

### 1. 게임 방 전체 조회
**GET** `/rooms`

멀티 게임 방 목록을 조회합니다.

**Query Parameters**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| page | Integer | O | 페이지 번호 (0부터 시작) |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": [
    {
      "gameRoomId": 1,
      "title": "초보 환영 방",
      "gameMode": "ROADVIEW",
      "gameType": "SOLO",
      "maxPlayers": 4,
      "currentPlayerCount": 2,
      "hostNickname": "홍길동",
      "privateRoom": false,
      "gameRoomStatus": "WAITING"
    }
  ]
}
```

**응답 필드 설명**
- `gameMode`: 게임 모드 (ROADVIEW, PHOTO)
- `gameType`: 매치 타입 (SOLO: 개인전, TEAM: 팀전)
- `gameRoomStatus`: 방 상태 (WAITING: 대기중, PLAYING: 게임중, FINISHED: 종료)

---

### 2. 게임 방 생성
**POST** `/rooms`

새로운 멀티 게임 방을 생성합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**
```json
{
  "title": "초보 환영 방",
  "password": "",
  "gameModeKey": "ROADVIEW",
  "playerMatchTypeKey": "SOLO",
  "maxPlayers": 4,
  "privateRoom": false
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | O | 방 제목 |
| password | String | X | 방 비밀번호 (비공개 방인 경우) |
| gameModeKey | String | O | 게임 모드 (ROADVIEW, PHOTO) |
| playerMatchTypeKey | String | O | 매치 타입 (SOLO, TEAM) |
| maxPlayers | Integer | O | 최대 플레이어 수 (2~8) |
| privateRoom | Boolean | O | 비공개 방 여부 |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "gameRoomId": 1,
    "title": "초보 환영 방",
    "gameModeKey": "ROADVIEW",
    "playerMatchTypeKey": "SOLO",
    "maxPlayers": 4
  }
}
```

---

### 3. 게임 방 상세 조회
**GET** `/rooms/{roomId}`

게임 방의 상세 정보를 조회합니다.

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| roomId | Long | 게임 방 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "gameRoomId": 1,
    "title": "초보 환영 방",
    "gameMode": "ROADVIEW",
    "gameType": "SOLO",
    "maxPlayers": 4,
    "hostNickname": "홍길동",
    "privateRoom": false,
    "gameRoomStatus": "WAITING",
    "players": [
      {
        "playerId": 1,
        "nickname": "홍길동",
        "isHost": true,
        "isReady": true
      }
    ]
  }
}
```

---

### 4. 게임 방 수정
**PUT** `/rooms/{roomId}`

게임 방 설정을 수정합니다 (방장만 가능).

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| roomId | Long | 게임 방 ID |

**Request Body**
```json
{
  "title": "고수만 오세요",
  "password": "1234",
  "gameModeKey": "ROADVIEW",
  "playerMatchTypeKey": "SOLO",
  "privateRoom": true,
  "teamCount": 2
}
```

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "gameRoomId": 1,
    "title": "고수만 오세요",
    "gameModeKey": "ROADVIEW",
    "playerMatchTypeKey": "SOLO",
    "maxPlayers": 4
  }
}
```

---

### 5. 게임 방 참여
**POST** `/rooms/{roomId}/players`

게임 방에 참여합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| roomId | Long | 게임 방 ID |

**Request Body**
```json
{
  "password": "1234"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| password | String | X | 비공개 방인 경우 비밀번호 |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

### 6. 게임 방 퇴장
**DELETE** `/rooms/{roomId}/players`

게임 방에서 퇴장합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| roomId | Long | 게임 방 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

**참고사항**
- 방장이 퇴장하면 방이 삭제되거나 다른 플레이어에게 방장이 이전될 수 있습니다.

---

### 7. 플레이어 강퇴
**DELETE** `/rooms/{roomId}/players/kick`

게임 방에서 플레이어를 강퇴시킵니다 (방장만 가능).

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| roomId | Long | 게임 방 ID |

**Request Body**
```json
{
  "targetPlayerId": 2
}
```

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

## 멀티 게임 플레이

### 1. 멀티 로드뷰 개인전 게임 시작
**POST** `/rooms/{roomId}/roadview/games/solo`

멀티 로드뷰 개인전 게임을 시작합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| roomId | String | 게임 방 ID |

**Request Body**
```json
{
  "gameRoomId": 1,
  "playerMatchTypeKey": "SOLO",
  "totalRounds": 5,
  "timeLimit": 60
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| gameRoomId | Long | O | 게임 방 ID |
| playerMatchTypeKey | String | O | 매치 타입 (SOLO) |
| totalRounds | Integer | O | 총 라운드 수 (1~10) |
| timeLimit | Integer | X | 라운드당 제한 시간 (초) |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "gameId": 1,
    "totalRounds": 5,
    "currentRound": 1,
    "roundInfo": {
      "roundId": 1,
      "targetLat": "37.5665",
      "targetLng": "126.9780",
      "markerImageUrl": "https://example.com/marker.png"
    },
    "gamePlayers": [
      {
        "playerId": 1,
        "nickname": "홍길동",
        "score": 0
      }
    ]
  }
}
```

---

### 2. 멀티 로드뷰 다음 라운드
**POST** `/rooms/{roomId}/roadview/games/{gameId}/rounds`

멀티 로드뷰 게임의 다음 라운드를 시작합니다.

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| roomId | String | 게임 방 ID |
| gameId | Long | 게임 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "gameId": 1,
    "currentRound": 2,
    "roundInfo": {
      "roundId": 2,
      "targetLat": "35.1796",
      "targetLng": "129.0756",
      "markerImageUrl": "https://example.com/marker.png"
    }
  }
}
```

---

## 게임 정답 제출

### 1. 로드뷰 개인전 정답 제출
**POST** `/rooms/{roomId}/games/{gameId}/rounds/{roundId}/submissions/player`

로드뷰 개인전 게임에서 플레이어가 정답을 제출합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| roomId | String | 게임 방 ID |
| gameId | Long | 게임 ID |
| roundId | Long | 라운드 ID |

**Request Body**
```json
{
  "lat": 37.5665,
  "lng": 126.9780,
  "timeToAnswer": 45.5
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| lat | Double | O | 제출한 위도 |
| lng | Double | O | 제출한 경도 |
| timeToAnswer | Double | O | 답변 시간 (밀리초) |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

**참고사항**
- 모든 플레이어가 제출하면 라운드가 종료됩니다.
- 제한 시간 내에 제출하지 않으면 0점 처리됩니다.
- 제출 후 WebSocket을 통해 실시간 결과가 전송됩니다.

---

### 2. 로드뷰 팀전 정답 제출
**POST** `/rooms/{roomId}/games/{gameId}/rounds/{roundId}/submissions/team`

로드뷰 팀전 게임에서 팀이 정답을 제출합니다.

> 🚧 **개발 중**: 이 기능은 현재 개발 중입니다.

---

## WebSocket API

### 1. 글로벌 로비 채팅

**구독 (Subscribe)**
```
/topic/lobby
```

**메시지 전송 (Send)**
```
/app/chat.message.lobby
```

**메시지 형식**
```json
{
  "message": "안녕하세요!",
  "nickname": "홍길동"
}
```

**로비 입장**
```
/app/chat.join.lobby
```

**로비 퇴장**
```
/app/chat.leave.lobby
```

---

### 2. 게임 방 채팅

**구독 (Subscribe)**
```
/topic/room/{roomId}/chat
```

**메시지 전송 (Send)**
```
/app/room.{roomId}.chat
```

**메시지 형식**
```json
{
  "message": "준비 완료!",
  "nickname": "홍길동"
}
```

---

### 3. 게임 방 플레이어 목록

**구독 (Subscribe)**
```
/topic/room/{roomId}/playerList
```

플레이어가 입장/퇴장하거나 상태가 변경되면 자동으로 업데이트가 전송됩니다.

**업데이트 메시지 형식**
```json
{
  "players": [
    {
      "playerId": 1,
      "nickname": "홍길동",
      "isHost": true,
      "isReady": true,
      "team": "TEAM_A"
    }
  ]
}
```

---

### 4. 팀 변경

**메시지 전송 (Send)**
```
/app/room.{roomId}.switchTeam
```

**메시지 형식**
```json
{
  "team": "TEAM_B"
}
```

**참고사항**
- 팀전 모드에서만 사용 가능합니다.
- 팀은 자동으로 균형이 맞춰집니다.

---

## WebSocket 연결 설정

### 엔드포인트
```
ws://localhost:8080/ws
```

### SockJS 사용 예시 (JavaScript)
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
  'Authorization': 'Bearer ' + accessToken
}, function(frame) {
  console.log('Connected: ' + frame);
  
  // 로비 채팅 구독
  stompClient.subscribe('/topic/lobby', function(message) {
    console.log('Received: ' + message.body);
  });
  
  // 메시지 전송
  stompClient.send('/app/chat.message.lobby', {}, JSON.stringify({
    message: '안녕하세요!',
    nickname: '홍길동'
  }));
});
```

**참고사항**
- WebSocket 연결 시 Authorization 헤더에 JWT 토큰이 필요합니다.
- STOMP 프로토콜을 사용합니다.
- SockJS를 통한 fallback이 지원됩니다.

