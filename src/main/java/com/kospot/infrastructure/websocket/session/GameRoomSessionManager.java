package com.kospot.infrastructure.websocket.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.infrastructure.exception.object.domain.WebSocketHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.constants.WebSocketChannelConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomSessionManager {

    // Redis Key Patterns
    private static final String ROOM_PLAYERS_KEY = "game:room:%s:players";
    private static final String ROOM_BANNED_KEY = "game:room:%s:banned";
    private static final String ROOM_HOST_KEY = "game:room:%s:host";
    private static final String ROOM_SETTINGS_KEY = "game:room:%s:settings";
    private static final String PLAYER_SESSION_KEY = "game:player:%s:session";
    private static final String SESSION_SUBSCRIPTIONS_KEY = "game:session:%s:subscriptions";
    private static final String SESSION_ROOM_KEY = "game:session:%s:room";

    // Expiry Settings
    private static final int SESSION_EXPIRY_HOURS = 24;
    private static final int ROOM_DATA_EXPIRY_HOURS = 12;

    // URL Pattern for Room ID extraction
    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("/room/(\\d+)/");

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final MemberAdaptor memberAdaptor;
    private final GameRoomAdaptor gameRoomAdaptor;
    private final ObjectMapper objectMapper;

    /**
     * WebSocket 구독 처리 및 플레이어 추가
     */
    public void addSubscription(WebSocketMemberPrincipal principal, String roomId, String destination, String sessionId) {
        try {
            // 세션의 구독 정보 저장
            String sessionKey = String.format(SESSION_SUBSCRIPTIONS_KEY, sessionId);
            redisTemplate.opsForSet().add(sessionKey, destination);
            redisTemplate.expire(sessionKey, SESSION_EXPIRY_HOURS, TimeUnit.HOURS);

            // 플레이어 세션 매핑
            String playerSessionKey = String.format(PLAYER_SESSION_KEY, principal.getMemberId());
            redisTemplate.opsForValue().set(playerSessionKey, sessionId, SESSION_EXPIRY_HOURS, TimeUnit.HOURS);

            // 세션-룸 매핑
            String sessionRoomKey = String.format(SESSION_ROOM_KEY, sessionId);
            redisTemplate.opsForValue().set(sessionRoomKey, roomId, SESSION_EXPIRY_HOURS, TimeUnit.HOURS);

            // 게임방 플레이어 목록 구독인 경우 플레이어 추가
            if (destination.contains("/players") || destination.contains("/playerList")) {
                addPlayerToRoom(principal, Long.parseLong(roomId));
            }

            log.info("Added subscription - MemberId: {}, RoomId: {}, Destination: {}", 
                    principal.getMemberId(), roomId, destination);

        } catch (Exception e) {
            log.error("Failed to add subscription - MemberId: {}, RoomId: {}, Error: {}", 
                    principal.getMemberId(), roomId, e.getMessage());
            throw new WebSocketHandler(ErrorStatus._BAD_REQUEST);
        }
    }

    /**
     * 플레이어를 게임방에 추가
     */
    private void addPlayerToRoom(WebSocketMemberPrincipal principal, Long roomId) {
        try {
            // 데이터베이스에서 멤버 정보 조회
            Member member = memberAdaptor.queryById(principal.getMemberId());
            GameRoom gameRoom = gameRoomAdaptor.queryById(roomId);

            // 플레이어가 이미 다른 방에 있는지 확인
            if (member.isAlreadyInGameRoom() && !member.getGameRoomId().equals(roomId)) {
                throw new WebSocketHandler(ErrorStatus.GAME_ROOM_MEMBER_ALREADY_IN_ROOM);
            }

            // 방이 가득 찬 경우 체크
            if (gameRoom.getCurrentPlayerCount() >= gameRoom.getMaxPlayers()) {
                throw new WebSocketHandler(ErrorStatus.GAME_ROOM_IS_FULL);
            }

            // 강퇴된 플레이어인지 확인
            if (isPlayerBanned(roomId.toString(), principal.getMemberId())) {
                throw new WebSocketHandler(ErrorStatus.GAME_ROOM_CANNOT_JOIN_NOW);
            }

            // Redis에 플레이어 정보 저장
            String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
            GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.from(member);
            String playerJson = objectMapper.writeValueAsString(playerInfo);
            
            redisTemplate.opsForHash().put(roomKey, member.getId().toString(), playerJson);
            redisTemplate.expire(roomKey, ROOM_DATA_EXPIRY_HOURS, TimeUnit.HOURS);

            // 데이터베이스 업데이트 (멤버의 게임방 정보)
            if (!member.isAlreadyInGameRoom()) {
                member.joinGameRoom(roomId);
            }

            // 다른 플레이어들에게 입장 알림
            notifyPlayerJoined(roomId.toString(), playerInfo);

            log.info("Player joined room - MemberId: {}, RoomId: {}, Nickname: {}", 
                    member.getId(), roomId, member.getNickname());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize player info - MemberId: {}, RoomId: {}", 
                    principal.getMemberId(), roomId);
            throw new WebSocketHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Failed to add player to room - MemberId: {}, RoomId: {}, Error: {}", 
                    principal.getMemberId(), roomId, e.getMessage());
            throw e;
        }
    }

    /**
     * 플레이어를 게임방에서 제거
     */
    public void removePlayerFromRoom(String sessionId) {
        try {
            // 세션에서 룸 ID 조회
            String sessionRoomKey = String.format(SESSION_ROOM_KEY, sessionId);
            String roomId = redisTemplate.opsForValue().get(sessionRoomKey);
            
            if (roomId == null) {
                log.warn("No room found for session: {}", sessionId);
                return;
            }

            // 세션에서 플레이어 ID 조회
            Long memberId = getMemberIdFromSession(sessionId);
            if (memberId == null) {
                log.warn("No member found for session: {}", sessionId);
                return;
            }

            removePlayerFromRoom(Long.parseLong(roomId), memberId);

        } catch (Exception e) {
            log.error("Failed to remove player from room - SessionId: {}, Error: {}", 
                    sessionId, e.getMessage());
        }
    }

    /**
     * 플레이어를 게임방에서 제거 (멤버 ID로)
     */
    public void removePlayerFromRoom(Long roomId, Long memberId) {
        try {
            String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
            
            // Redis에서 플레이어 정보 조회 후 삭제
            String playerJson = (String) redisTemplate.opsForHash().get(roomKey, memberId.toString());
            if (playerJson != null) {
                GameRoomPlayerInfo playerInfo = objectMapper.readValue(playerJson, GameRoomPlayerInfo.class);
                redisTemplate.opsForHash().delete(roomKey, memberId.toString());

                // 데이터베이스 업데이트
                Member member = memberAdaptor.queryById(memberId);
                member.leaveGameRoom();

                // 다른 플레이어들에게 퇴장 알림
                notifyPlayerLeft(roomId.toString(), playerInfo);

                log.info("Player left room - MemberId: {}, RoomId: {}, Nickname: {}", 
                        memberId, roomId, playerInfo.getNickname());
            }

            // 세션 정보 정리
            cleanupPlayerSession(memberId);

        } catch (Exception e) {
            log.error("Failed to remove player from room - MemberId: {}, RoomId: {}, Error: {}", 
                    memberId, roomId, e.getMessage());
        }
    }

    /**
     * 플레이어 강퇴
     */
    public void kickPlayer(Long roomId, Long hostMemberId, Long targetMemberId) {
        try {
            // 호스트 권한 확인
            GameRoom gameRoom = gameRoomAdaptor.queryById(roomId);
            Member host = memberAdaptor.queryById(hostMemberId);
            gameRoom.validateHost(host);

            // 강퇴 대상 확인
            Member targetMember = memberAdaptor.queryById(targetMemberId);
            if (!targetMember.getGameRoomId().equals(roomId)) {
                throw new WebSocketHandler(ErrorStatus._BAD_REQUEST);
            }

            // 강퇴 목록에 추가
            String bannedKey = String.format(ROOM_BANNED_KEY, roomId);
            redisTemplate.opsForSet().add(bannedKey, targetMemberId.toString());
            redisTemplate.expire(bannedKey, ROOM_DATA_EXPIRY_HOURS, TimeUnit.HOURS);

            // 플레이어 제거
            removePlayerFromRoom(roomId, targetMemberId);

            // 강퇴 알림
            notifyPlayerKicked(roomId.toString(), targetMember);

            log.info("Player kicked - HostId: {}, TargetId: {}, RoomId: {}", 
                    hostMemberId, targetMemberId, roomId);

        } catch (Exception e) {
            log.error("Failed to kick player - HostId: {}, TargetId: {}, RoomId: {}, Error: {}", 
                    hostMemberId, targetMemberId, roomId, e.getMessage());
            throw e;
        }
    }

    /**
     * 게임방의 현재 플레이어 목록 조회
     */
    public List<GameRoomPlayerInfo> getRoomPlayers(String roomId) {
        try {
            String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
            Map<Object, Object> players = redisTemplate.opsForHash().entries(roomKey);
            
            return players.values().stream()
                    .map(playerJson -> {
                        try {
                            return objectMapper.readValue((String) playerJson, GameRoomPlayerInfo.class);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to deserialize player info: {}", playerJson);
                            return null;
                        }
                    })
                    .filter(player -> player != null)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Failed to get room players - RoomId: {}, Error: {}", roomId, e.getMessage());
            return List.of();
        }
    }

    /**
     * destination URL에서 룸 ID 추출
     */
    public String extractRoomId(String destination) {
        if (destination == null) {
            return null;
        }
        
        Matcher matcher = ROOM_ID_PATTERN.matcher(destination);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    /**
     * 플레이어 입장 알림
     */
    private void notifyPlayerJoined(String roomId, GameRoomPlayerInfo playerInfo) {
        String destination = String.format(WebSocketChannelConstants.GAME_ROOM_PLAYER_LIST, roomId);
        
        GameRoomNotification notification = GameRoomNotification.builder()
                .type("PLAYER_JOINED")
                .roomId(roomId)
                .playerInfo(playerInfo)
                .players(getRoomPlayers(roomId))
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend(destination, notification);
        log.info("Sent player joined notification - RoomId: {}, PlayerId: {}", roomId, playerInfo.getMemberId());
    }

    /**
     * 플레이어 퇴장 알림
     */
    private void notifyPlayerLeft(String roomId, GameRoomPlayerInfo playerInfo) {
        String destination = String.format(WebSocketChannelConstants.GAME_ROOM_PLAYER_LIST, roomId);
        
        GameRoomNotification notification = GameRoomNotification.builder()
                .type("PLAYER_LEFT")
                .roomId(roomId)
                .playerInfo(playerInfo)
                .players(getRoomPlayers(roomId))
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend(destination, notification);
        log.info("Sent player left notification - RoomId: {}, PlayerId: {}", roomId, playerInfo.getMemberId());
    }

    /**
     * 플레이어 강퇴 알림
     */
    private void notifyPlayerKicked(String roomId, Member kickedMember) {
        String destination = String.format(WebSocketChannelConstants.GAME_ROOM_PLAYER_LIST, roomId);
        
        GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.from(kickedMember);
        GameRoomNotification notification = GameRoomNotification.builder()
                .type("PLAYER_KICKED")
                .roomId(roomId)
                .playerInfo(playerInfo)
                .players(getRoomPlayers(roomId))
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend(destination, notification);
        log.info("Sent player kicked notification - RoomId: {}, PlayerId: {}", roomId, kickedMember.getId());
    }

    /**
     * 강퇴된 플레이어인지 확인
     */
    private boolean isPlayerBanned(String roomId, Long memberId) {
        String bannedKey = String.format(ROOM_BANNED_KEY, roomId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(bannedKey, memberId.toString()));
    }

    /**
     * 세션에서 멤버 ID 조회
     */
    private Long getMemberIdFromSession(String sessionId) {
        try {
            String subscriptionsKey = String.format(SESSION_SUBSCRIPTIONS_KEY, sessionId);
            Set<String> subscriptions = redisTemplate.opsForSet().members(subscriptionsKey);
            
            if (subscriptions != null) {
                for (String subscription : subscriptions) {
                    // 구독 정보에서 플레이어 세션 키를 역추적
                    Set<String> playerKeys = redisTemplate.keys(String.format(PLAYER_SESSION_KEY, "*"));
                    for (String playerKey : playerKeys) {
                        String storedSessionId = redisTemplate.opsForValue().get(playerKey);
                        if (sessionId.equals(storedSessionId)) {
                            // 키에서 멤버 ID 추출
                            String[] parts = playerKey.split(":");
                            return Long.parseLong(parts[parts.length - 2]); // game:player:{memberId}:session
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get member ID from session: {}", sessionId);
            return null;
        }
    }

    /**
     * 플레이어 세션 정보 정리
     */
    private void cleanupPlayerSession(Long memberId) {
        try {
            String playerSessionKey = String.format(PLAYER_SESSION_KEY, memberId);
            String sessionId = redisTemplate.opsForValue().get(playerSessionKey);
            
            if (sessionId != null) {
                // 세션 관련 키들 삭제
                String sessionSubscriptionsKey = String.format(SESSION_SUBSCRIPTIONS_KEY, sessionId);
                String sessionRoomKey = String.format(SESSION_ROOM_KEY, sessionId);
                
                redisTemplate.delete(playerSessionKey);
                redisTemplate.delete(sessionSubscriptionsKey);
                redisTemplate.delete(sessionRoomKey);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup player session - MemberId: {}", memberId);
        }
    }

    /**
     * 게임방 플레이어 정보 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class GameRoomPlayerInfo {
        private Long memberId;
        private String nickname;
        private String markerImageUrl;
        private boolean isHost;
        private Long joinedAt;

        public static GameRoomPlayerInfo from(Member member) {
            return GameRoomPlayerInfo.builder()
                    .memberId(member.getId())
                    .nickname(member.getNickname())
                    .markerImageUrl(member.getEquippedMarkerImage() != null ? 
                            member.getEquippedMarkerImage().getImageUrl() : null)
                    .isHost(false) // 호스트 여부는 별도로 설정
                    .joinedAt(System.currentTimeMillis())
                    .build();
        }
    }

    /**
     * 게임방 알림 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class GameRoomNotification {
        private String type; // PLAYER_JOINED, PLAYER_LEFT, PLAYER_KICKED
        private String roomId;
        private GameRoomPlayerInfo playerInfo;
        private List<GameRoomPlayerInfo> players; // 현재 방의 모든 플레이어
        private Long timestamp;
    }
}
