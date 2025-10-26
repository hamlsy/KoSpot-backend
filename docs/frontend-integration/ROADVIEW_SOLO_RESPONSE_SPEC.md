# 로드뷰 개인전 Response 명세

> API 및 WebSocket 메시지의 모든 Response 구조 상세 명세서

## 📋 목차
1. [공통 응답 형식](#1-공통-응답-형식)
2. [게임방 관련 Response](#2-게임방-관련-response)
3. [게임 진행 Response](#3-게임-진행-response)
4. [WebSocket 메시지 Response](#4-websocket-메시지-response)
5. [TypeScript 인터페이스](#5-typescript-인터페이스)

---

## 1. 공통 응답 형식

### 1.1 ApiResponseDto

모든 REST API는 다음 형식으로 응답합니다.

```typescript
interface ApiResponseDto<T> {
  status: number;        // 상태 코드 (2000: 성공)
  isSuccess: boolean;    // 성공 여부
  message: string;       // 메시지
  result: T | null;      // 실제 데이터
}
```

**성공 응답 예시:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": {
    // 실제 데이터
  }
}
```

**에러 응답 예시:**
```json
{
  "status": 4004,
  "isSuccess": false,
  "message": "게임방을 찾을 수 없습니다.",
  "result": null
}
```

### 1.2 상태 코드

| 코드 | 의미 | 설명 |
|-----|------|------|
| 2000 | SUCCESS | 요청 성공 |
| 4000 | BAD_REQUEST | 잘못된 요청 |
| 4001 | UNAUTHORIZED | 인증 실패 |
| 4003 | FORBIDDEN | 권한 없음 |
| 4004 | NOT_FOUND | 리소스를 찾을 수 없음 |
| 4009 | CONFLICT | 이미 존재하는 리소스 |
| 5000 | INTERNAL_SERVER_ERROR | 서버 내부 오류 |

---

## 2. 게임방 관련 Response

### 2.1 GameRoomResponse

게임방 생성/수정 시 반환

```typescript
interface GameRoomResponse {
  roomId: number;
  title: string;
  hostId: number;
  currentPlayers: number;
  maxPlayers: number;
  status: RoomStatus;     // "WAITING" | "PLAYING" | "FINISHED"
}
```

**예시:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": {
    "roomId": 1,
    "title": "재미있는 게임방",
    "hostId": 123,
    "currentPlayers": 1,
    "maxPlayers": 8,
    "status": "WAITING"
  }
}
```

### 2.2 FindGameRoomResponse

게임방 목록 조회 시 반환

```typescript
interface FindGameRoomResponse {
  roomId: number;
  title: string;
  hostNickname: string;
  currentPlayers: number;
  maxPlayers: number;
  gameMode: GameMode;         // "ROADVIEW" | "PHOTO"
  matchType: MatchType;       // "SOLO" | "TEAM"
  isPrivate: boolean;
  status: RoomStatus;
}
```

**예시:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": [
    {
      "roomId": 1,
      "title": "초보 환영",
      "hostNickname": "플레이어1",
      "currentPlayers": 3,
      "maxPlayers": 8,
      "gameMode": "ROADVIEW",
      "matchType": "SOLO",
      "isPrivate": false,
      "status": "WAITING"
    },
    {
      "roomId": 2,
      "title": "고수만",
      "hostNickname": "프로게이머",
      "currentPlayers": 6,
      "maxPlayers": 8,
      "gameMode": "ROADVIEW",
      "matchType": "SOLO",
      "isPrivate": false,
      "status": "PLAYING"
    }
  ]
}
```

### 2.3 GameRoomDetailResponse

게임방 상세 조회 시 반환

```typescript
interface GameRoomDetailResponse {
  roomId: number;
  title: string;
  hostId: number;
  currentPlayers: number;
  maxPlayers: number;
  gameMode: GameMode;
  matchType: MatchType;
  status: RoomStatus;
  isPrivate: boolean;
  players: GameRoomPlayerResponse[];
}

interface GameRoomPlayerResponse {
  playerId: number;
  nickname: string;
  markerImageUrl: string;
  isHost: boolean;
  isReady: boolean;
  team?: string;              // 팀전인 경우
}
```

**예시:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": {
    "roomId": 1,
    "title": "재미있는 게임방",
    "hostId": 123,
    "currentPlayers": 3,
    "maxPlayers": 8,
    "gameMode": "ROADVIEW",
    "matchType": "SOLO",
    "status": "WAITING",
    "isPrivate": false,
    "players": [
      {
        "playerId": 123,
        "nickname": "플레이어1",
        "markerImageUrl": "https://example.com/marker1.png",
        "isHost": true,
        "isReady": true
      },
      {
        "playerId": 124,
        "nickname": "플레이어2",
        "markerImageUrl": "https://example.com/marker2.png",
        "isHost": false,
        "isReady": false
      }
    ]
  }
}
```

---

## 3. 게임 진행 Response

### 3.1 StartPlayerGame Response

게임 시작 시 반환 (첫 라운드 정보 포함)

```typescript
interface StartPlayerGameResponse {
  gameId: number;
  totalRounds: number;
  currentRound: number;
  roundInfo: RoundInfo;
  gamePlayers: GamePlayerResponse[];
}

interface RoundInfo {
  roundId: number;
  roundNumber: number;
  targetLat: number;
  targetLng: number;
}

interface GamePlayerResponse {
  playerId: number;
  nickname: string;
  markerImageUrl: string;
  totalScore: number;
  roundRank: number;
}
```

**예시:**
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
      },
      {
        "playerId": 203,
        "nickname": "플레이어3",
        "markerImageUrl": "https://example.com/marker3.png",
        "totalScore": 0,
        "roundRank": 0
      }
    ]
  }
}
```

**필드 설명:**
- `gameId`: 생성된 게임 ID (이후 API 호출 및 WebSocket 구독에 사용)
- `totalRounds`: 전체 라운드 수
- `currentRound`: 현재 라운드 번호 (1부터 시작)
- `roundInfo.roundId`: 라운드 ID (정답 제출 시 필요)
- `roundInfo.targetLat/Lng`: 정답 좌표 (로드뷰 표시용)
- `gamePlayers`: 게임에 참여하는 모든 플레이어 정보
- `roundRank`: 현재 순위 (초기값 0)

### 3.2 NextRound Response

다음 라운드 시작 시 반환

```typescript
interface NextRoundResponse {
  gameId: number;
  currentRound: number;
  roundInfo: RoundInfo;
}
```

**예시:**
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

**참고:**
- 일반적으로 이 API는 직접 호출하지 않습니다
- WebSocket `/topic/game/{roomId}/round/start` 채널을 통해 자동으로 브로드캐스트됩니다

### 3.3 Submission Response

정답 제출 시 반환

```typescript
type SubmissionResponse = "SUCCESS";
```

**예시:**
```json
{
  "status": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": "SUCCESS"
}
```

**참고:**
- 제출 즉시 성공 응답만 반환
- 실제 점수 및 결과는 라운드 종료 후 WebSocket을 통해 수신

---

## 4. WebSocket 메시지 Response

### 4.1 TimerStartMessage

라운드 타이머 시작 메시지

```typescript
interface TimerStartMessage {
  roundId: string;
  gameMode: "ROADVIEW" | "PHOTO";
  serverStartTimeMs: number;
  durationMs: number;
  serverTimestamp: number;
}
```

**예시:**
```json
{
  "roundId": "1001",
  "gameMode": "ROADVIEW",
  "serverStartTimeMs": 1698765432000,
  "durationMs": 60000,
  "serverTimestamp": 1698765432000
}
```

**필드 설명:**
- `roundId`: 라운드 ID
- `gameMode`: 게임 모드
- `serverStartTimeMs`: 서버에서 타이머가 시작된 정확한 시각 (epoch ms)
- `durationMs`: 타이머 지속 시간 (밀리초)
- `serverTimestamp`: 현재 서버 시간 (클라이언트 동기화용)

**클라이언트 타이머 동기화:**
```javascript
const timeDiff = message.serverTimestamp - Date.now();
const startTime = message.serverStartTimeMs;
const duration = message.durationMs;

setInterval(() => {
  const now = Date.now() + timeDiff;
  const elapsed = now - startTime;
  const remaining = Math.max(0, duration - elapsed);
  
  // UI 업데이트
  updateTimer(remaining);
}, 100);
```

### 4.2 TimerSyncMessage

타이머 동기화 메시지 (주기적 전송)

```typescript
interface TimerSyncMessage {
  roundId: string;
  remainingTimeMs: number;
  serverTimestamp: number;
  isFinalCountDown: boolean;
}
```

**예시:**
```json
{
  "roundId": "1001",
  "remainingTimeMs": 45000,
  "serverTimestamp": 1698765447000,
  "isFinalCountDown": false
}
```

**필드 설명:**
- `remainingTimeMs`: 남은 시간 (밀리초)
- `isFinalCountDown`: 마지막 10초 여부 (UI 강조 표시용)

### 4.3 PlayerSubmissionMessage

플레이어 제출 알림 메시지

```typescript
interface PlayerSubmissionMessage {
  playerId: number;
  roundId: number;
  timestamp: string;      // ISO 8601 형식
}
```

**예시:**
```json
{
  "playerId": 202,
  "roundId": 1001,
  "timestamp": "2024-10-31T12:34:56.789Z"
}
```

**용도:**
- "플레이어 X가 제출했습니다" UI 표시
- 제출 카운터 업데이트 (예: "3/5명 제출 완료")

### 4.4 RoundResult (PlayerResult)

라운드 결과 메시지

```typescript
interface RoundResult {
  roundNumber: number;
  targetLat: number;
  targetLng: number;
  playerSubmissionResults: SubmissionResult[];
  playerTotalResults: PlayerResult[];
}

interface SubmissionResult {
  lat: number;
  lng: number;
  distance: number;           // 미터
  timeToAnswer: number;       // 밀리초
  earnedScore: number;        // 이번 라운드 획득 점수
}

interface PlayerResult {
  playerId: number;
  nickname: string;
  markerImageUrl: string;
  totalScore: number;         // 누적 점수
  roundRank: number;          // 현재 순위
}
```

**예시:**
```json
{
  "roundNumber": 1,
  "targetLat": 37.5665,
  "targetLng": 126.9780,
  "playerSubmissionResults": [
    {
      "lat": 37.5670,
      "lng": 126.9785,
      "distance": 123.45,
      "timeToAnswer": 45230.5,
      "earnedScore": 8500
    },
    {
      "lat": 37.5680,
      "lng": 126.9790,
      "distance": 234.56,
      "timeToAnswer": 52100.0,
      "earnedScore": 7200
    },
    {
      "lat": 37.5650,
      "lng": 126.9770,
      "distance": 178.90,
      "timeToAnswer": 38500.0,
      "earnedScore": 8000
    }
  ],
  "playerTotalResults": [
    {
      "playerId": 201,
      "nickname": "플레이어1",
      "markerImageUrl": "https://example.com/marker1.png",
      "totalScore": 8500,
      "roundRank": 1
    },
    {
      "playerId": 203,
      "nickname": "플레이어3",
      "markerImageUrl": "https://example.com/marker3.png",
      "totalScore": 8000,
      "roundRank": 2
    },
    {
      "playerId": 202,
      "nickname": "플레이어2",
      "markerImageUrl": "https://example.com/marker2.png",
      "totalScore": 7200,
      "roundRank": 3
    }
  ]
}
```

**필드 설명:**
- `targetLat/Lng`: 정답 좌표
- `playerSubmissionResults`: 각 플레이어의 제출 결과
  - `distance`: 정답과의 직선 거리 (미터)
  - `timeToAnswer`: 제출 소요 시간 (밀리초)
  - `earnedScore`: 이번 라운드에서 획득한 점수
- `playerTotalResults`: 누적 점수 및 순위 (점수 내림차순 정렬)
  - `totalScore`: 현재까지 누적된 총 점수
  - `roundRank`: 현재 전체 순위

**주의사항:**
- `playerSubmissionResults`와 `playerTotalResults`의 순서는 다를 수 있습니다
- `playerTotalResults`는 누적 점수 순으로 정렬되어 있습니다
- 제출하지 않은 플레이어는 포함되지 않거나 0점 처리됩니다

### 4.5 RoundTransitionTimerMessage

라운드 전환 대기 타이머 메시지

```typescript
interface RoundTransitionTimerMessage {
  nextRoundStartTimeMs: number;
  serverTimestamp: number;
  isLastRound: boolean;
}
```

**예시:**
```json
{
  "nextRoundStartTimeMs": 1698765442000,
  "serverTimestamp": 1698765432000,
  "isLastRound": false
}
```

**필드 설명:**
- `nextRoundStartTimeMs`: 다음 라운드가 시작될 정확한 시각 (epoch ms)
- `serverTimestamp`: 현재 서버 시간
- `isLastRound`: 마지막 라운드 여부 (true면 게임 종료 예정)

**클라이언트 카운트다운:**
```javascript
const timeDiff = message.serverTimestamp - Date.now();
const nextStartTime = message.nextRoundStartTimeMs;

setInterval(() => {
  const now = Date.now() + timeDiff;
  const remaining = nextStartTime - now;
  const seconds = Math.ceil(remaining / 1000);
  
  // 10, 9, 8, ... , 1 카운트다운
  updateCountdown(seconds);
  
  if (remaining <= 0) {
    clearInterval(interval);
  }
}, 100);
```

### 4.6 GameFinishedMessage

게임 종료 메시지

```typescript
interface GameFinishedMessage {
  gameId: number;
  message: string;
  timestamp: number;
}
```

**예시:**
```json
{
  "gameId": 100,
  "message": "게임이 종료되었습니다.",
  "timestamp": 1698765532000
}
```

**필드 설명:**
- `gameId`: 종료된 게임 ID
- `message`: 종료 메시지
- `timestamp`: 게임 종료 시각

**처리:**
- 이 메시지를 받으면 게임 종료 화면으로 전환
- 최종 순위는 마지막 라운드 결과에 포함되어 있음

---

## 5. TypeScript 인터페이스

### 5.1 전체 인터페이스 정의

```typescript
// ==================== Enums ====================

enum RoomStatus {
  WAITING = "WAITING",
  PLAYING = "PLAYING",
  FINISHED = "FINISHED"
}

enum GameMode {
  ROADVIEW = "ROADVIEW",
  PHOTO = "PHOTO"
}

enum MatchType {
  SOLO = "SOLO",
  TEAM = "TEAM"
}

// ==================== Common ====================

interface ApiResponseDto<T> {
  status: number;
  isSuccess: boolean;
  message: string;
  result: T | null;
}

// ==================== Game Room ====================

interface GameRoomResponse {
  roomId: number;
  title: string;
  hostId: number;
  currentPlayers: number;
  maxPlayers: number;
  status: RoomStatus;
}

interface FindGameRoomResponse {
  roomId: number;
  title: string;
  hostNickname: string;
  currentPlayers: number;
  maxPlayers: number;
  gameMode: GameMode;
  matchType: MatchType;
  isPrivate: boolean;
  status: RoomStatus;
}

interface GameRoomDetailResponse {
  roomId: number;
  title: string;
  hostId: number;
  currentPlayers: number;
  maxPlayers: number;
  gameMode: GameMode;
  matchType: MatchType;
  status: RoomStatus;
  isPrivate: boolean;
  players: GameRoomPlayerResponse[];
}

interface GameRoomPlayerResponse {
  playerId: number;
  nickname: string;
  markerImageUrl: string;
  isHost: boolean;
  isReady: boolean;
  team?: string;
}

// ==================== Game ====================

interface StartPlayerGameResponse {
  gameId: number;
  totalRounds: number;
  currentRound: number;
  roundInfo: RoundInfo;
  gamePlayers: GamePlayerResponse[];
}

interface NextRoundResponse {
  gameId: number;
  currentRound: number;
  roundInfo: RoundInfo;
}

interface RoundInfo {
  roundId: number;
  roundNumber: number;
  targetLat: number;
  targetLng: number;
}

interface GamePlayerResponse {
  playerId: number;
  nickname: string;
  markerImageUrl: string;
  totalScore: number;
  roundRank: number;
}

// ==================== WebSocket Messages ====================

interface TimerStartMessage {
  roundId: string;
  gameMode: GameMode;
  serverStartTimeMs: number;
  durationMs: number;
  serverTimestamp: number;
}

interface TimerSyncMessage {
  roundId: string;
  remainingTimeMs: number;
  serverTimestamp: number;
  isFinalCountDown: boolean;
}

interface PlayerSubmissionMessage {
  playerId: number;
  roundId: number;
  timestamp: string;
}

interface RoundResult {
  roundNumber: number;
  targetLat: number;
  targetLng: number;
  playerSubmissionResults: SubmissionResult[];
  playerTotalResults: PlayerResult[];
}

interface SubmissionResult {
  lat: number;
  lng: number;
  distance: number;
  timeToAnswer: number;
  earnedScore: number;
}

interface PlayerResult {
  playerId: number;
  nickname: string;
  markerImageUrl: string;
  totalScore: number;
  roundRank: number;
}

interface RoundTransitionTimerMessage {
  nextRoundStartTimeMs: number;
  serverTimestamp: number;
  isLastRound: boolean;
}

interface GameFinishedMessage {
  gameId: number;
  message: string;
  timestamp: number;
}

// ==================== Room WebSocket Messages ====================

interface PlayerJoinedMessage {
  type: "PLAYER_JOINED";
  player: GameRoomPlayerResponse;
  currentPlayers: number;
}

interface PlayerLeftMessage {
  type: "PLAYER_LEFT";
  playerId: number;
  currentPlayers: number;
}

interface GameStartedMessage {
  type: "GAME_STARTED";
  roomStatus: RoomStatus;
  gameId: number;
}

interface ChatMessage {
  senderId: number;
  senderNickname: string;
  message: string;
  timestamp: number;
}
```

### 5.2 API 함수 타입 정의

```typescript
// REST API Functions

type GetGameRoomsResponse = ApiResponseDto<FindGameRoomResponse[]>;
type CreateGameRoomResponse = ApiResponseDto<GameRoomResponse>;
type GetGameRoomDetailResponse = ApiResponseDto<GameRoomDetailResponse>;
type JoinGameRoomResponse = ApiResponseDto<"SUCCESS">;
type LeaveGameRoomResponse = ApiResponseDto<"SUCCESS">;

type StartGameResponse = ApiResponseDto<StartPlayerGameResponse>;
type NextRoundResponse = ApiResponseDto<NextRoundResponse>;
type SubmitAnswerResponse = ApiResponseDto<"SUCCESS">;

// WebSocket Subscription Handlers

type TimerStartHandler = (message: TimerStartMessage) => void;
type TimerSyncHandler = (message: TimerSyncMessage) => void;
type SubmissionHandler = (message: PlayerSubmissionMessage) => void;
type RoundResultHandler = (message: RoundResult) => void;
type RoundTransitionHandler = (message: RoundTransitionTimerMessage) => void;
type NextRoundHandler = (message: NextRoundResponse) => void;
type GameFinishedHandler = (message: GameFinishedMessage) => void;

type PlayerJoinedHandler = (message: PlayerJoinedMessage) => void;
type PlayerLeftHandler = (message: PlayerLeftMessage) => void;
type GameStartedHandler = (message: GameStartedMessage) => void;
type ChatMessageHandler = (message: ChatMessage) => void;
```

### 5.3 사용 예제

```typescript
// API 호출 예제
async function startGame(
  roomId: string,
  settings: {
    gameRoomId: number;
    totalRounds: number;
    timeLimit: number;
  }
): Promise<StartPlayerGameResponse | null> {
  try {
    const response = await fetch(`/rooms/${roomId}/roadview/games/solo`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        gameRoomId: settings.gameRoomId,
        playerMatchTypeKey: 'SOLO',
        totalRounds: settings.totalRounds,
        timeLimit: settings.timeLimit
      })
    });

    const data: StartGameResponse = await response.json();
    
    if (data.isSuccess && data.result) {
      return data.result;
    }
    
    return null;
  } catch (error) {
    console.error('게임 시작 실패:', error);
    return null;
  }
}

// WebSocket 구독 예제
function setupGameSubscriptions(
  ws: WebSocketClient,
  roomId: string,
  handlers: {
    onTimerStart: TimerStartHandler;
    onRoundResult: RoundResultHandler;
    onRoundTransition: RoundTransitionHandler;
    onNextRound: NextRoundHandler;
    onGameFinished: GameFinishedHandler;
  }
) {
  ws.subscribe(`/topic/game/${roomId}/timer`, handlers.onTimerStart);
  ws.subscribe(`/topic/game/${roomId}/round/result`, handlers.onRoundResult);
  ws.subscribe(`/topic/game/${roomId}/round/transition`, handlers.onRoundTransition);
  ws.subscribe(`/topic/game/${roomId}/round/start`, handlers.onNextRound);
  ws.subscribe(`/topic/game/${roomId}/game/finished`, handlers.onGameFinished);
}
```

---

## 6. 필드 상세 설명

### 6.1 좌표 관련

- **lat (latitude)**: 위도, -90.0 ~ 90.0
- **lng (longitude)**: 경도, -180.0 ~ 180.0
- **targetLat/targetLng**: 정답 좌표 (서버에서 랜덤 생성)

### 6.2 시간 관련

- **serverStartTimeMs**: 타이머 시작 시각 (Unix epoch milliseconds)
- **durationMs**: 지속 시간 (밀리초)
- **timeToAnswer**: 정답 제출까지 걸린 시간 (밀리초)
- **serverTimestamp**: 현재 서버 시간 (Unix epoch milliseconds)
- **timestamp**: ISO 8601 형식 문자열 (예: "2024-10-31T12:34:56.789Z")

### 6.3 점수 관련

- **earnedScore**: 해당 라운드에서 획득한 점수
- **totalScore**: 현재까지 누적된 총 점수
- **distance**: 정답과의 거리 (미터)

### 6.4 순위 관련

- **roundRank**: 현재 전체 순위 (1부터 시작)
- 점수가 같은 경우 동일 순위 부여

---

## 7. 데이터 변환 유틸리티

### 7.1 시간 변환

```typescript
// 밀리초 → 초
function msToSeconds(ms: number): number {
  return Math.floor(ms / 1000);
}

// 밀리초 → 분:초 형식
function msToMinutesSeconds(ms: number): string {
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

// ISO 8601 → Date
function parseTimestamp(timestamp: string): Date {
  return new Date(timestamp);
}
```

### 7.2 거리 변환

```typescript
// 미터 → 킬로미터
function metersToKm(meters: number): number {
  return meters / 1000;
}

// 거리 포맷팅
function formatDistance(meters: number): string {
  if (meters < 1000) {
    return `${meters.toFixed(0)}m`;
  }
  return `${(meters / 1000).toFixed(2)}km`;
}
```

### 7.3 점수 포맷팅

```typescript
// 천 단위 구분자
function formatScore(score: number): string {
  return score.toLocaleString('ko-KR');
}
```

---

## 8. 검증 및 에러 처리

### 8.1 응답 검증

```typescript
function validateApiResponse<T>(data: ApiResponseDto<T>): T {
  if (!data.isSuccess) {
    throw new Error(`API Error: ${data.message} (code: ${data.status})`);
  }
  
  if (data.result === null) {
    throw new Error('API returned null result');
  }
  
  return data.result;
}

// 사용 예
try {
  const response: StartGameResponse = await fetch(...).then(r => r.json());
  const gameData = validateApiResponse(response);
  
  // gameData는 StartPlayerGameResponse 타입으로 보장됨
  console.log('Game started:', gameData.gameId);
} catch (error) {
  console.error('Failed to start game:', error.message);
}
```

### 8.2 WebSocket 메시지 검증

```typescript
function validateWebSocketMessage<T>(message: any, requiredFields: string[]): T {
  for (const field of requiredFields) {
    if (!(field in message)) {
      throw new Error(`Missing required field: ${field}`);
    }
  }
  return message as T;
}

// 사용 예
ws.subscribe('/topic/game/1/timer', (rawMessage) => {
  try {
    const message = validateWebSocketMessage<TimerStartMessage>(
      rawMessage,
      ['roundId', 'gameMode', 'serverStartTimeMs', 'durationMs', 'serverTimestamp']
    );
    
    handleTimerStart(message);
  } catch (error) {
    console.error('Invalid message:', error);
  }
});
```

---

## 9. 참고 문서

- [REST API 가이드](./ROADVIEW_SOLO_API_GUIDE.md)
- [WebSocket 가이드](./ROADVIEW_SOLO_WEBSOCKET_GUIDE.md)
- [통합 플로우 가이드](./ROADVIEW_SOLO_INTEGRATION_FLOW.md)

