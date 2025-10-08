# KoSpot Redis 데이터 구조 및 저장 방식 가이드

## 📋 목차
1. [Redis 키 패턴 개요](#redis-키-패턴-개요)
2. [게임방 관련 데이터 구조](#게임방-관련-데이터-구조)
3. [세션 관리 데이터 구조](#세션-관리-데이터-구조)
4. [게임 타이머 데이터 구조](#게임-타이머-데이터-구조)
5. [글로벌 데이터 구조](#글로벌-데이터-구조)
6. [데이터 저장 방식 및 직렬화](#데이터-저장-방식-및-직렬화)
7. [TTL 및 만료 정책](#ttl-및-만료-정책)
8. [성능 최적화 팁](#성능-최적화-팁)

---

## 🔑 Redis 키 패턴 개요

### 네임스페이스 구조
```
game:          # 게임 관련 데이터
├── room:      # 게임방 데이터
├── player:    # 플레이어 데이터
├── session:   # 세션 데이터
└── timer:     # 타이머 데이터

session:       # 세션 컨텍스트 데이터
├── ctx:       # 세션 컨텍스트

lobby:         # 로비 데이터
chat:          # 채팅 데이터
```

### 키 네이밍 규칙
- **소문자와 콜론(:)만 사용**
- **의미있는 네임스페이스 구분**
- **버전 관리 가능한 구조**
- **계층적 구조로 구성**

---

## 🏠 게임방 관련 데이터 구조

### 1. 게임방 플레이어 정보

**키 패턴**: `game:room:{roomId}:players`  
**데이터 타입**: Hash  
**TTL**: 12시간  
**설명**: 게임방에 참여 중인 모든 플레이어의 정보

#### 저장 구조
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

#### 필드 설명
| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `memberId` | Long | 플레이어 고유 ID | 12345 |
| `nickname` | String | 플레이어 닉네임 | "Player1" |
| `markerImageUrl` | String | 마커 이미지 URL | "https://example.com/marker1.png" |
| `team` | String | 팀 정보 (RED/BLUE/null) | "RED" |
| `isHost` | Boolean | 방장 여부 | true |
| `joinedAt` | Long | 입장 시간 (Unix timestamp) | 1703123456789 |

#### 사용 예시
```java
// 플레이어 추가
gameRoomRedisService.addPlayerToRoom("123", playerInfo);

// 플레이어 제거
gameRoomRedisService.removePlayerFromRoom("123", 12345L);

// 플레이어 목록 조회
List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers("123");

// 팀 변경
gameRoomRedisService.switchTeam("123", 12345L, "BLUE");
```

### 2. 게임방 강퇴 목록

**키 패턴**: `game:room:{roomId}:banned`  
**데이터 타입**: Set  
**TTL**: 12시간  
**설명**: 해당 게임방에서 강퇴된 플레이어 목록

#### 저장 구조
```json
["12345", "67890", "11111"]
```

#### 사용 예시
```java
// 강퇴 목록에 추가
redisTemplate.opsForSet().add("game:room:123:banned", "12345");

// 강퇴 여부 확인
boolean isBanned = redisTemplate.opsForSet().isMember("game:room:123:banned", "12345");

// 강퇴 목록 조회
Set<String> bannedPlayers = redisTemplate.opsForSet().members("game:room:123:banned");
```

---

## 🔐 세션 관리 데이터 구조

### 1. 플레이어 세션 매핑

**키 패턴**: `game:player:{memberId}:session`  
**데이터 타입**: String  
**TTL**: 24시간  
**설명**: 플레이어 ID와 WebSocket 세션 ID 매핑

#### 저장 구조
```json
"abc123def456ghi789"
```

#### 사용 예시
```java
// 세션 정보 저장
gameRoomRedisService.saveSessionInfo("abc123", "123", "/topic/room/123/playerList", 12345L);

// 세션 ID 조회
String sessionId = redisTemplate.opsForValue().get("game:player:12345:session");

// 세션 정리
gameRoomRedisService.cleanupPlayerSession(12345L);
```

### 2. 세션 구독 정보

**키 패턴**: `game:session:{sessionId}:subscriptions`  
**데이터 타입**: Set  
**TTL**: 24시간  
**설명**: 특정 세션이 구독 중인 채널 목록

#### 저장 구조
```json
["/topic/room/123/playerList", "/topic/room/123/chat", "/topic/room/123/settings"]
```

#### 사용 예시
```java
// 구독 정보 저장
String sessionKey = String.format("game:session:%s:subscriptions", sessionId);
redisTemplate.opsForSet().add(sessionKey, "/topic/room/123/playerList");

// 구독 목록 조회
Set<String> subscriptions = redisTemplate.opsForSet().members(sessionKey);

// 구독 해제
redisTemplate.opsForSet().remove(sessionKey, "/topic/room/123/playerList");
```

### 3. 세션-룸 매핑

**키 패턴**: `game:session:{sessionId}:room`  
**데이터 타입**: String  
**TTL**: 24시간  
**설명**: 세션이 현재 참여 중인 게임방 ID

#### 저장 구조
```json
"123"
```

#### 사용 예시
```java
// 룸 매핑 저장
String sessionRoomKey = String.format("game:session:%s:room", sessionId);
redisTemplate.opsForValue().set(sessionRoomKey, "123", 24, TimeUnit.HOURS);

// 룸 ID 조회
String roomId = gameRoomRedisService.getRoomIdFromSession(sessionId);
```

### 4. 세션 컨텍스트

**키 패턴**: `session:ctx:{sessionId}`  
**데이터 타입**: Hash  
**TTL**: 24시간  
**설명**: 세션별 추가 컨텍스트 정보

#### 저장 구조
```json
{
  "roomId": "123",
  "chatId": "global",
  "gameType": "ROADVIEW",
  "lastActivity": "1703123456789"
}
```

#### 사용 예시
```java
// 컨텍스트 저장
sessionContextRedisService.setAttr(sessionId, "roomId", "123");
sessionContextRedisService.setAttr(sessionId, "gameType", "ROADVIEW");

// 컨텍스트 조회
String roomId = sessionContextRedisService.getAttr(sessionId, "roomId", String.class);
String gameType = sessionContextRedisService.getAttr(sessionId, "gameType", String.class);
```

---

## ⏰ 게임 타이머 데이터 구조

### 1. 게임 라운드 정보

**키 패턴**: `game:room:{roomId}:round:{roundId}`  
**데이터 타입**: Hash  
**TTL**: 게임 종료 시  
**설명**: 특정 라운드의 상세 정보

#### 저장 구조
```json
{
  "roundId": "round1",
  "gameType": "ROADVIEW",
  "question": "이곳은 어디인가요?",
  "imageUrl": "https://example.com/roadview.jpg",
  "timeLimit": 30,
  "startTime": 1703123456789,
  "endTime": 1703123486789,
  "status": "ACTIVE",
  "correctAnswer": {
    "latitude": 37.5665,
    "longitude": 126.9780,
    "address": "서울특별시 중구"
  }
}
```

#### 필드 설명
| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `roundId` | String | 라운드 고유 ID | "round1" |
| `gameType` | String | 게임 타입 | "ROADVIEW" |
| `question` | String | 문제 내용 | "이곳은 어디인가요?" |
| `imageUrl` | String | 이미지 URL | "https://example.com/roadview.jpg" |
| `timeLimit` | Integer | 제한 시간(초) | 30 |
| `startTime` | Long | 시작 시간 | 1703123456789 |
| `endTime` | Long | 종료 시간 | 1703123486789 |
| `status` | String | 라운드 상태 | "ACTIVE" |
| `correctAnswer` | Object | 정답 정보 | 위 예시 참조 |

### 2. 활성 라운드 목록

**키 패턴**: `game:room:{roomId}:active:rounds`  
**데이터 타입**: Set  
**TTL**: 게임 종료 시  
**설명**: 현재 활성화된 라운드 ID 목록

#### 저장 구조
```json
["round1", "round2", "round3"]
```

### 3. 플레이어 라운드 정보

**키 패턴**: `player:{memberId}:room:{roomId}:round`  
**데이터 타입**: Hash  
**TTL**: 게임 종료 시  
**설명**: 플레이어의 라운드별 답안 및 점수

#### 저장 구조
```json
{
  "round1": {
    "answer": {
      "latitude": 37.5000,
      "longitude": 127.0000,
      "address": "서울특별시 강남구"
    },
    "score": 850,
    "submittedAt": 1703123460000,
    "distance": 5.2
  },
  "round2": {
    "answer": {
      "latitude": 37.6000,
      "longitude": 127.1000,
      "address": "서울특별시 서초구"
    },
    "score": 920,
    "submittedAt": 1703123490000,
    "distance": 2.1
  }
}
```

---

## 🌐 글로벌 데이터 구조

### 1. 로비 사용자 목록

**키 패턴**: `lobby:users`  
**데이터 타입**: Set  
**TTL**: 24시간  
**설명**: 현재 로비에 있는 사용자 목록

#### 저장 구조
```json
["12345", "67890", "11111", "22222"]
```

### 2. 최근 채팅 메시지

**키 패턴**: `chat:recent:{chatId}`  
**데이터 타입**: List  
**TTL**: 7일  
**설명**: 최근 채팅 메시지 목록 (최대 100개)

#### 저장 구조
```json
[
  {
    "messageId": "msg1",
    "senderId": 12345,
    "senderNickname": "Player1",
    "content": "안녕하세요!",
    "timestamp": 1703123456789,
    "chatType": "GLOBAL"
  },
  {
    "messageId": "msg2",
    "senderId": 67890,
    "senderNickname": "Player2",
    "content": "게임 같이 하실 분?",
    "timestamp": 1703123456790,
    "chatType": "GLOBAL"
  }
]
```

---

## 💾 데이터 저장 방식 및 직렬화

### JSON 직렬화 설정

```java
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        // Key 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value 직렬화 - JSON
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}
```

### 데이터 저장 예시

```java
@Service
public class GameRoomRedisService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // 플레이어 정보 저장
    public void addPlayerToRoom(String roomId, GameRoomPlayerInfo playerInfo) {
        String roomKey = String.format("game:room:%s:players", roomId);
        String playerJson = objectMapper.writeValueAsString(playerInfo);
        
        // Hash 구조로 저장: roomKey -> memberId -> playerJson
        redisTemplate.opsForHash().put(roomKey, 
            playerInfo.getMemberId().toString(), playerJson);
        
        // TTL 설정
        redisTemplate.expire(roomKey, 12, TimeUnit.HOURS);
    }
    
    // 플레이어 정보 조회
    public List<GameRoomPlayerInfo> getRoomPlayers(String roomId) {
        String roomKey = String.format("game:room:%s:players", roomId);
        Map<Object, Object> players = redisTemplate.opsForHash().entries(roomKey);
        
        return players.values().stream()
            .map(playerJson -> {
                try {
                    return objectMapper.readValue((String) playerJson, 
                        GameRoomPlayerInfo.class);
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize player info: {}", playerJson);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
```

---

## ⏳ TTL 및 만료 정책

### TTL 설정 규칙

| 데이터 타입 | TTL | 이유 |
|-------------|-----|------|
| **세션 데이터** | 24시간 | 사용자 세션 유지 |
| **게임방 데이터** | 12시간 | 게임방 활성 상태 유지 |
| **게임 타이머** | 게임 종료 시 | 게임 진행 중 데이터 보존 |
| **채팅 데이터** | 7일 | 최근 메시지 보관 |
| **로비 데이터** | 24시간 | 로비 상태 유지 |

### TTL 설정 예시

```java
// 세션 데이터 TTL 설정
redisTemplate.expire("game:player:12345:session", 24, TimeUnit.HOURS);

// 게임방 데이터 TTL 설정
redisTemplate.expire("game:room:123:players", 12, TimeUnit.HOURS);

// 채팅 데이터 TTL 설정
redisTemplate.expire("chat:recent:global", 7, TimeUnit.DAYS);
```

### 자동 정리 작업

```java
@Service
public class RedisCleanupService {
    
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void cleanupExpiredData() {
        // 빈 게임방 정리
        cleanupEmptyRooms();
        
        // 만료된 세션 정리
        cleanupExpiredSessions();
        
        // 오래된 채팅 데이터 정리
        cleanupOldChatMessages();
    }
    
    @Async("taskExecutor")
    public void cleanupEmptyRooms() {
        Set<String> roomKeys = redisTemplate.keys("game:room:*:players");
        
        for (String key : roomKeys) {
            if (redisTemplate.opsForHash().size(key) == 0) {
                redisTemplate.delete(key);
                log.info("Cleaned up empty room: {}", key);
            }
        }
    }
}
```

---

## 🚀 성능 최적화 팁

### 1. Hash 구조 활용

```java
// ✅ 좋은 예: Hash 구조 사용
String roomKey = "game:room:123:players";
redisTemplate.opsForHash().put(roomKey, "12345", playerJson);

// ❌ 나쁜 예: 개별 키 사용
redisTemplate.opsForValue().set("game:room:123:player:12345", playerJson);
```

### 2. 배치 작업 사용

```java
// 여러 플레이어 정보를 한 번에 저장
public void addMultiplePlayers(String roomId, List<GameRoomPlayerInfo> players) {
    String roomKey = String.format("game:room:%s:players", roomId);
    Map<Object, Object> playerMap = new HashMap<>();
    
    for (GameRoomPlayerInfo player : players) {
        String playerJson = objectMapper.writeValueAsString(player);
        playerMap.put(player.getMemberId().toString(), playerJson);
    }
    
    // 한 번의 작업으로 모든 플레이어 저장
    redisTemplate.opsForHash().putAll(roomKey, playerMap);
}
```

### 3. 파이프라인 사용

```java
// 여러 Redis 명령을 파이프라인으로 실행
public void updatePlayerInfo(String roomId, Long memberId, GameRoomPlayerInfo playerInfo) {
    String roomKey = String.format("game:room:%s:players", roomId);
    String playerJson = objectMapper.writeValueAsString(playerInfo);
    
    redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
        connection.hSet(roomKey.getBytes(), memberId.toString().getBytes(), playerJson.getBytes());
        connection.expire(roomKey.getBytes(), 12 * 3600); // 12시간
        return null;
    });
}
```

### 4. 메모리 사용량 최적화

```java
// 불필요한 데이터 제거
public void cleanupPlayerData(String roomId, Long memberId) {
    String roomKey = String.format("game:room:%s:players", roomId);
    
    // 플레이어 정보 제거
    redisTemplate.opsForHash().delete(roomKey, memberId.toString());
    
    // 빈 방인 경우 전체 키 삭제
    if (redisTemplate.opsForHash().size(roomKey) == 0) {
        redisTemplate.delete(roomKey);
    }
}
```

---

## 📊 모니터링 및 디버깅

### Redis 키 조회 명령어

```bash
# 모든 게임방 키 조회
redis-cli KEYS "game:room:*"

# 특정 게임방 플레이어 조회
redis-cli HGETALL "game:room:123:players"

# 세션 정보 조회
redis-cli GET "game:player:12345:session"

# TTL 확인
redis-cli TTL "game:room:123:players"
```

### 로깅 및 통계

```java
@Service
public class RedisMonitoringService {
    
    public void logRedisStatistics() {
        Set<String> roomKeys = redisTemplate.keys("game:room:*:players");
        
        int totalRooms = roomKeys.size();
        int totalPlayers = 0;
        int emptyRooms = 0;
        
        for (String key : roomKeys) {
            Long playerCount = redisTemplate.opsForHash().size(key);
            totalPlayers += playerCount.intValue();
            
            if (playerCount == 0) {
                emptyRooms++;
            }
        }
        
        log.info("Redis Statistics - Total Rooms: {}, Total Players: {}, Empty Rooms: {}, Avg Players/Room: {}",
                totalRooms, totalPlayers, emptyRooms,
                totalRooms > 0 ? (double) totalPlayers / totalRooms : 0);
    }
}
```

---

## 📚 참고 자료

- [Redis Data Types](https://redis.io/docs/data-types/)
- [Redis Hash Commands](https://redis.io/commands/hash/)
- [Redis TTL Commands](https://redis.io/commands/ttl/)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)

---

**📝 문서 버전**: 1.0  
**🔄 최종 업데이트**: 2024-01-01  
**👥 작성자**: KoSpot Backend Team

