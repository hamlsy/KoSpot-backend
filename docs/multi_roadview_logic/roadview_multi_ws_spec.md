# 로드뷰 개인전 WebSocket 브로드캐스트 명세

본 문서는 현재 코드 기준으로 **로드뷰 멀티 개인전** 흐름에서 서버가 브로드캐스트하는 WebSocket 메시지를 정리한다. 각 항목은 실제 구현 클래스, 메시지 DTO, 전송 채널, 샘플 페이로드를 포함한다.

---

## 1. 게임방 관련 브로드캐스트 (`GameRoomNotificationService`)

### 1.1 공통 사항
- 서비스: `GameRoomNotificationService`
- 채널 Prefix: `GameRoomChannelConstants.PREFIX_GAME_ROOM` (`/topic/room/`)
- 공통 페이로드: `GameRoomNotification`  
  ```0:0:src/main/java/com/kospot/domain/multi/room/vo/GameRoomNotification.java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameRoomNotification {
    private String type;                  // GameRoomNotificationType enum name
    private String roomId;
    private GameRoomPlayerInfo playerInfo; // 개별 이벤트일 때
    private List<GameRoomPlayerInfo> players; // 전체 갱신일 때
    private Long timestamp;              // epoch millis
}
  ```
- `GameRoomPlayerInfo` 구조:
  ```0:0:src/main/java/com/kospot/domain/multi/room/vo/GameRoomPlayerInfo.java
@Data
@Builder
public class GameRoomPlayerInfo {
    private Long memberId;
    private String nickname;
    private String markerImageUrl;
    private String team;
    private boolean isHost;
    private Long joinedAt;
}
  ```

### 1.2 이벤트별 정리
| 메서드 | 채널 | type | 추가 설명 |
| --- | --- | --- | --- |
| `notifyPlayerJoined` | `/topic/room/{roomId}/playerList` | `PLAYER_JOINED` | 새 플레이어 입장 |
| `notifyPlayerLeft` | `/topic/room/{roomId}/playerList` | `PLAYER_LEFT` | 플레이어 퇴장 |
| `notifyPlayerKicked` | `/topic/room/{roomId}/playerList` | `PLAYER_KICKED` | 강퇴 |
| `notifyPlayerListUpdated` | `/topic/room/{roomId}/playerList` | `PLAYER_LIST_UPDATED` | 전체 목록 동기화 |
| `notifyGameStarted` | `/topic/room/{roomId}/playerList` | `GAME_STARTED` | 게임 시작 이벤트 |

#### 예시 페이로드
```json
{
  "type": "PLAYER_JOINED",
  "roomId": "123",
  "playerInfo": {
    "memberId": 52,
    "nickname": "가좌로_헌터",
    "markerImageUrl": "https://cdn....png",
    "team": null,
    "host": false,
    "joinedAt": 1731093105123
  },
  "players": null,
  "timestamp": 1731093105123
}
```

### 1.3 방 설정 변경
- 메서드: `notifyRoomSettingsChanged`
- 채널: `/topic/room/{roomId}/settings`
- 페이로드: JSON 문자열(`GameRoomUpdateMessage`)
  ```0:0:src/main/java/com/kospot/presentation/multi/gameroom/dto/message/GameRoomUpdateMessage.java
@Getter
@Builder
public class GameRoomUpdateMessage {
    private final String roomId;
    private final String title;
    private final String gameModeKey;
    private final String playerMatchTypeKey;
    private final boolean privateRoom;
    private final int teamCount;
}
  ```
- 예시:
```json
{
  "roomId": "123",
  "title": "가좌로 개인전",
  "gameModeKey": "ROADVIEW",
  "playerMatchTypeKey": "SOLO",
  "privateRoom": false,
  "teamCount": 1
}
```

---

## 2. 제출 관련 브로드캐스트 (`SubmissionNotificationService`)
- 메서드: `notifySubmissionReceived`
- 채널: `/topic/game/{gameId}/roadview/submissions/player`
- 페이로드: `PlayerSubmissionMessage`
  ```0:0:src/main/java/com/kospot/application/multi/submission/websocket/message/PlayerSubmissionMessage.java
@Getter
@Builder
public class PlayerSubmissionMessage {
    private Long playerId;
    private Long roundId;
    private Instant timestamp;
}
  ```
- 예시:
```json
{
  "playerId": 77,
  "roundId": 201,
  "timestamp": "2025-11-09T13:12:23.112345Z"
}
```

---

## 3. 라운드 진행/결과 브로드캐스트 (`GameRoundNotificationService`)

### 3.1 라운드 시작
- 메서드: `broadcastRoundStart`
- 채널: `/topic/game/{roomId}/round/start`
- 페이로드: `MultiRoadViewGameResponse.StartPlayerGame` (게임 시작) 또는 `MultiRoadViewGameResponse.NextRound` (다음 라운드)
  ```0:0:src/main/java/com/kospot/presentation/multi/game/dto/response/MultiRoadViewGameResponse.java
@Data
@Builder
public static class StartPlayerGame {
    private Long gameId;
    private int totalRounds;
    private int currentRound;
    private RoadViewRoundResponse.Info roundInfo;
    private List<GamePlayerResponse> gamePlayers;
}
  ```
  ```0:0:src/main/java/com/kospot/presentation/multi/game/dto/response/MultiRoadViewGameResponse.java
@Data
@Builder
public static class NextRound {
    private Long gameId;
    private int currentRound;
    private RoadViewRoundResponse.Info roundInfo;
}
  ```
- `roundInfo` & `gamePlayers` 구조:
  ```0:0:src/main/java/com/kospot/presentation/multi/round/dto/response/RoadViewRoundResponse.java
@Data
@Builder
public static class Info {
    private Long roundId;
    private int roundNumber;
    private double targetLat;
    private double targetLng;
}
  ```
  ```0:0:src/main/java/com/kospot/presentation/multi/gamePlayer/dto/response/GamePlayerResponse.java
@Builder
public record GamePlayerResponse(Long playerId, Long memberId, String nickname,
                                 String markerImageUrl, Double totalScore,
                                 Integer roundRank, boolean host) { ... }
  ```
- 예시:
```json
{
  "gameId": 13,
  "totalRounds": 5,
  "currentRound": 1,
  "roundInfo": {
    "roundId": 201,
    "roundNumber": 1,
    "targetLat": 37.5564,
    "targetLng": 126.9723
  },
  "gamePlayers": [
    {
      "playerId": 301,
      "memberId": 52,
      "nickname": "가좌로_헌터",
      "markerImageUrl": "https://cdn....png",
      "totalScore": 0.0,
      "roundRank": null,
      "host": true
    }
  ]
}
```

### 3.2 라운드 결과
- 메서드: `broadcastRoundResults`
- 채널: `/topic/game/{roomId}/round/result`
- 페이로드: `RoadViewRoundResponse.PlayerResult`
  ```0:0:src/main/java/com/kospot/presentation/multi/round/dto/response/RoadViewRoundResponse.java
@Data
@Builder
public static class PlayerResult {
    private int roundNumber;
    private double targetLat;
    private double targetLng;
    private List<SubmissionResponse.RoadViewPlayer> playerSubmissionResults;
    private List<GamePlayerResponse> playerTotalResults;
}
  ```
- 제출 결과 DTO:
  ```0:0:src/main/java/com/kospot/presentation/multi/submission/dto/response/SubmissionResponse.java
@Builder
public static class RoadViewPlayer {
    private Long submissionId;
    private Long playerId;
    private Double distance;
    private Double score;
    private Integer rank;
    private Double submittedLat;
    private Double submittedLng;
    private Long submittedAt;
}
  ```
- 예시:
```json
{
  "roundNumber": 1,
  "targetLat": 37.5564,
  "targetLng": 126.9723,
  "playerSubmissionResults": [
    {
      "submissionId": 5001,
      "playerId": 301,
      "distance": 12.3,
      "score": 4800.0,
      "rank": 1,
      "submittedLat": 37.5565,
      "submittedLng": 126.9722,
      "submittedAt": 1731093265123
    }
  ],
  "playerTotalResults": [
    {
      "playerId": 301,
      "memberId": 52,
      "nickname": "가좌로_헌터",
      "markerImageUrl": "https://cdn....png",
      "totalScore": 4800.0,
      "roundRank": 1,
      "host": true
    }
  ]
}
```

### 3.3 게임 종료
- 메서드: `notifyGameFinishedWithResults`
- 채널: `/topic/game/{roomId}/game/finished`
- 페이로드: `MultiGameResponse.GameFinalResult`
  ```0:0:src/main/java/com/kospot/presentation/multi/game/dto/response/MultiGameResponse.java
@Getter
@Builder
public static class GameFinalResult {
    private Long gameId;
    private String message;
    private Long timestamp;
    private List<PlayerFinalResult> playerResults;
}
  ```
- `PlayerFinalResult` 구조:
```json
{
  "playerId": 301,
  "nickname": "가좌로_헌터",
  "markerImageUrl": "https://cdn....png",
  "totalScore": 15800.0,
  "finalRank": 1,
  "earnedPoint": 200
}
```

---

## 4. 타이머 브로드캐스트 (`GameTimerService`)

### 4.1 라운드 시작 타이머
- 메서드: `startRoundTimer`
- 채널: `/topic/game/{roomId}/timer/start`
- 페이로드: `TimerStartMessage`
  ```0:0:src/main/java/com/kospot/application/multi/timer/message/TimerStartMessage.java
@Data
@Builder
public class TimerStartMessage {
    private String roundId;
    private GameMode gameMode;
    private Long serverStartTimeMs;
    private Long durationMs;
    private Long serverTimestamp;
}
  ```

### 4.2 라운드 타이머 동기화
- 메서드: `scheduleTimerSync`
- 채널: `/topic/game/{roomId}/timer/sync`
- 페이로드: `TimerSyncMessage`
  ```0:0:src/main/java/com/kospot/application/multi/timer/message/TimerSyncMessage.java
@Data
@Builder
public class TimerSyncMessage {
    private String roundId;
    private long remainingTimeMs;
    private long serverTimestamp;
    private boolean isFinalCountDown;
}
  ```

### 4.3 라운드 전환 대기 타이머
- 메서드: `broadcastRoundTransitionTimer`
- 채널: `/topic/game/{roomId}/round/transition`
- 페이로드: `RoundTransitionTimerMessage`
  ```0:0:src/main/java/com/kospot/application/multi/timer/message/RoundTransitionTimerMessage.java
@Getter
@Builder
public class RoundTransitionTimerMessage {
    private final Long nextRoundStartTimeMs;
    private final Long serverTimestamp;
    private final Boolean isLastRound;
}
  ```

---

## 5. 기타 브로드캐스트

### 5.1 정기 플레이어 리스트 방송 (`GameRoomPlayersBroadcaster`)
- 10초마다 `/topic/room/{roomId}/playerList` 에 `GameRoomNotification`(`PLAYER_LIST_UPDATED`) 전송.

### 5.2 채팅 메시지 (참고)
- `ChatService` → `/topic/chat/room/{roomId}` 채널에 `ChatMessageResponse`.  
  로드뷰 개인전과 직접적 관련은 없으나 동일 게임룸에서 사용될 수 있다.

---

## 6. 프론트엔드 구독 체크리스트
1. `/topic/room/{roomId}/playerList`  
   - 플레이어 입퇴장, 강퇴, 목록 갱신, 게임 시작
2. `/topic/room/{roomId}/settings`  
   - 방 설정 변경 반영
3. `/topic/game/{gameId}/roadview/submissions/player`  
   - 실시간 제출 표시
4. `/topic/game/{roomId}/round/start`  
   - 라운드/게임 시작 컨텍스트 반영
5. `/topic/game/{roomId}/timer/start`, `/topic/game/{roomId}/timer/sync`  
   - 라운드 타이머 렌더링
6. `/topic/game/{roomId}/round/result`  
   - 라운드 결과 화면
7. `/topic/game/{roomId}/round/transition`  
   - 결과 → 다음 라운드까지 카운트다운
8. `/topic/game/{roomId}/game/finished`  
   - 최종 결과 화면

필요 시 추가 orchestrator 이벤트(카운트다운, 강제 이동 등)는 기존 채널 패턴을 확장하여 정의한다.

