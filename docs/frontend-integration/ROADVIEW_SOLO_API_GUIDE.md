# 로드뷰 개인전 REST API 명세

> 프론트엔드 연동을 위한 로드뷰 개인전 게임 REST API 상세 명세서

## 📋 목차
1. [게임방 관리 API](#1-게임방-관리-api)
2. [게임 진행 API](#2-게임-진행-api)
3. [정답 제출 API](#3-정답-제출-api)
4. [공통 응답 형식](#4-공통-응답-형식)

---

## 1. 게임방 관리 API

### 1.1 게임방 목록 조회
```http
GET /rooms?page={page}
```

**Query Parameters:**
- `page` (int): 페이지 번호 (0부터 시작)

**Response:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": [
    {
      "roomId": 1,
      "title": "방 제목",
      "hostNickname": "방장닉네임",
      "currentPlayers": 3,
      "maxPlayers": 8,
      "gameMode": "ROADVIEW",
      "matchType": "SOLO"
    }
  ]
}
```

### 1.2 게임방 생성
```http
POST /rooms
Content-Type: application/json
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "title": "방 제목",
  "maxPlayers": 8,
  "gameMode": "ROADVIEW",
  "matchType": "SOLO",
  "isPrivate": false,
  "password": null
}
```

**Response:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": {
    "roomId": 1,
    "title": "방 제목",
    "hostId": 123,
    "currentPlayers": 1,
    "maxPlayers": 8,
    "status": "WAITING"
  }
}
```

### 1.3 게임방 참여
```http
POST /rooms/{roomId}/players
Content-Type: application/json
Authorization: Bearer {token}
```

**Path Parameters:**
- `roomId` (Long): 게임방 ID

**Request Body:**
```json
{
  "password": null
}
```

**Response:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": "SUCCESS"
}
```

### 1.4 게임방 퇴장
```http
DELETE /rooms/{roomId}/players
Authorization: Bearer {token}
```

**Path Parameters:**
- `roomId` (Long): 게임방 ID

**Response:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": "SUCCESS"
}
```

### 1.5 게임방 상세 조회
```http
GET /rooms/{roomId}
```

**Path Parameters:**
- `roomId` (Long): 게임방 ID

**Response:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": {
    "roomId": 1,
    "title": "방 제목",
    "hostId": 123,
    "currentPlayers": 3,
    "maxPlayers": 8,
    "gameMode": "ROADVIEW",
    "matchType": "SOLO",
    "status": "WAITING",
    "players": [
      {
        "playerId": 123,
        "nickname": "플레이어1",
        "isHost": true,
        "isReady": true
      }
    ]
  }
}
```

---

## 2. 게임 진행 API

### 2.1 개인전 게임 시작 (첫 라운드)
```http
POST /rooms/{roomId}/roadview/games/solo
Content-Type: application/json
Authorization: Bearer {token}
```

**Path Parameters:**
- `roomId` (String): 게임방 ID

**Request Body:**
```json
{
  "gameRoomId": 1,
  "playerMatchTypeKey": "SOLO",
  "totalRounds": 5,
  "timeLimit": 60000
}
```

**Request Fields:**
- `gameRoomId` (Long): 게임방 ID
- `playerMatchTypeKey` (String): 매치 타입 ("SOLO" 또는 "TEAM")
- `totalRounds` (int): 총 라운드 수 (1~10)
- `timeLimit` (Integer): 라운드 제한 시간 (밀리초, nullable)

**Response:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": {
    "gameId": 100,
    "totalRounds": 5,
    "currentRound": 1,
    "roundInfo": {
      "roundId": 1001,
      "roundNumber": 1,
      "targetLat": 37.5665,
      "targetLng": 126.9780
    },
    "gamePlayers": [
      {
        "playerId": 201,
        "nickname": "플레이어1",
        "markerImageUrl": "https://example.com/marker1.png",
        "totalScore": 0,
        "roundRank": 0
      },
      {
        "playerId": 202,
        "nickname": "플레이어2",
        "markerImageUrl": "https://example.com/marker2.png",
        "totalScore": 0,
        "roundRank": 0
      }
    ]
  }
}
```

**동작:**
1. 게임 시작 권한 확인 (방장만 가능)
2. 게임 생성 및 첫 번째 라운드 생성
3. 모든 플레이어 정보 초기화
4. 라운드 타이머 시작
5. WebSocket을 통해 타이머 시작 메시지 브로드캐스트

### 2.2 다음 라운드 시작
```http
POST /rooms/{roomId}/roadview/games/{gameId}/rounds
Authorization: Bearer {token}
```

**Path Parameters:**
- `roomId` (String): 게임방 ID
- `gameId` (Long): 게임 ID

**Response:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": {
    "gameId": 100,
    "currentRound": 2,
    "roundInfo": {
      "roundId": 1002,
      "roundNumber": 2,
      "targetLat": 35.1796,
      "targetLng": 129.0756
    }
  }
}
```

**주의사항:**
- 이 API는 일반적으로 직접 호출하지 않습니다
- 라운드 전환 타이머(10초) 종료 후 자동으로 서버에서 처리됩니다
- WebSocket `/topic/game/{roomId}/round/start` 채널을 통해 자동으로 브로드캐스트됩니다

---

## 3. 정답 제출 API

### 3.1 개인전 정답 제출
```http
POST /rooms/{roomId}/games/{gameId}/rounds/{roundId}/submissions/player
Content-Type: application/json
Authorization: Bearer {token}
```

**Path Parameters:**
- `roomId` (String): 게임방 ID
- `gameId` (Long): 게임 ID
- `roundId` (Long): 라운드 ID

**Request Body:**
```json
{
  "lat": 37.5665,
  "lng": 126.9780,
  "timeToAnswer": 45230.5
}
```

**Request Fields:**
- `lat` (Double, required): 제출한 위도
- `lng` (Double, required): 제출한 경도
- `timeToAnswer` (Double, required): 응답 시간 (밀리초, 양수)

**Response:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": "SUCCESS"
}
```

**동작 프로세스:**
1. 제출 데이터 검증 (위도/경도/시간)
2. 정답 좌표와의 거리 계산
3. 점수 계산 (거리 + 응답시간 고려)
4. Redis에 제출 기록 저장
5. WebSocket을 통해 제출 알림 브로드캐스트
6. 모든 플레이어가 제출했는지 확인 (조기 종료 로직)

**제출 후 발생하는 이벤트:**
1. **제출 알림 브로드캐스트** 
   - 채널: `/topic/game/{gameId}/roadview/submissions/player`
   - 다른 플레이어들에게 "누가 제출했는지" 실시간 알림

2. **조기 종료 체크**
   - 모든 플레이어가 제출 완료 시 타이머 즉시 종료
   - 라운드 결과 즉시 브로드캐스트

---

## 4. 공통 응답 형식

### 4.1 성공 응답
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": { }
}
```

### 4.2 에러 응답
```json
{
  "status": 4000,
  "isSuccess": false,
  "message": "에러 메시지",
  "result": null
}
```

### 4.3 주요 에러 코드
| 상태 코드 | 설명 |
|---------|------|
| 2000 | 성공 |
| 4000 | 잘못된 요청 |
| 4001 | 인증 실패 |
| 4003 | 권한 없음 |
| 4004 | 리소스를 찾을 수 없음 |
| 4009 | 이미 존재하는 리소스 |
| 5000 | 서버 내부 오류 |

---

## 5. 인증

모든 API는 JWT 토큰 기반 인증을 사용합니다.

**Header:**
```
Authorization: Bearer {your_jwt_token}
```

**토큰 획득:**
- 별도의 인증 API를 통해 로그인 후 JWT 토큰을 발급받습니다.
- 토큰은 요청 헤더에 포함하여 전송합니다.

---

## 6. 주요 참고사항

### 6.1 게임 시작 조건
- 방장만 게임을 시작할 수 있습니다
- 최소 1명 이상의 플레이어가 필요합니다
- 게임방 상태가 `WAITING`이어야 합니다

### 6.2 라운드 제한시간
- `timeLimit`을 설정하지 않으면 기본값 적용 (60초)
- 밀리초 단위로 설정 (예: 60000 = 60초)
- 제한시간 종료 시 자동으로 라운드 종료

### 6.3 점수 계산
- 거리 점수: 정답 좌표와의 거리가 가까울수록 높은 점수
- 시간 보너스: 빠르게 제출할수록 추가 점수
- 순위 점수: 라운드별 순위에 따른 점수

### 6.4 게임 종료
- 모든 라운드 완료 시 자동으로 게임 종료
- WebSocket `/topic/game/{roomId}/game/finished` 채널로 종료 알림
- 최종 순위 및 점수 집계

---

## 7. 예제 시나리오

### 시나리오: 5라운드 개인전 게임 진행

1. **게임방 참여**
   ```
   POST /rooms/1/players
   ```

2. **게임 시작 (방장)**
   ```
   POST /rooms/1/roadview/games/solo
   Body: { gameRoomId: 1, totalRounds: 5, timeLimit: 60000 }
   ```

3. **첫 번째 라운드 정답 제출**
   ```
   POST /rooms/1/games/100/rounds/1001/submissions/player
   Body: { lat: 37.5665, lng: 126.9780, timeToAnswer: 45000 }
   ```

4. **라운드 결과 수신 (WebSocket)**
   - 채널: `/topic/game/1/round/result`
   - 모든 플레이어의 제출 결과, 점수, 순위 수신

5. **라운드 전환 대기 (10초)**
   - 채널: `/topic/game/1/round/transition`
   - 다음 라운드까지 카운트다운

6. **다음 라운드 자동 시작 (WebSocket)**
   - 채널: `/topic/game/1/round/start`
   - 2라운드 시작 알림 및 새 문제 수신

7. **3~5번 반복** (5라운드까지)

8. **게임 종료 (WebSocket)**
   - 채널: `/topic/game/1/game/finished`
   - 최종 순위 및 게임 종료 알림

---

## 8. 다음 문서

- [WebSocket 연동 가이드](./ROADVIEW_SOLO_WEBSOCKET_GUIDE.md) - WebSocket 채널 및 메시지 명세
- [통합 플로우 가이드](./ROADVIEW_SOLO_INTEGRATION_FLOW.md) - 전체 게임 플로우 및 프론트엔드 연동 예제

