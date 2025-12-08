package com.kospot.domain.multi.room.vo;

import com.kospot.domain.member.entity.Member;
import com.kospot.presentation.multi.room.dto.response.GameRoomPlayerResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 게임방 플레이어 정보 VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameRoomPlayerInfo {

    private Long memberId;

    private String nickname;

    private String markerImageUrl;

    private String team;

    private boolean isHost;

    private Long joinedAt;

    @Deprecated // 메서드 내부에서 사용될 때만
    public static GameRoomPlayerInfo from(Member member, boolean isHost) {
        return GameRoomPlayerInfo.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .markerImageUrl(member.getEquippedMarkerImage().getImageUrl()) // 직렬화 주의!
                .isHost(isHost) // 호스트 여부는 별도로 설정 필요
                .joinedAt(System.currentTimeMillis())
                .build();
    }

    public static GameRoomPlayerResponse toResponse(GameRoomPlayerInfo playerInfo) {
        return GameRoomPlayerResponse.builder()
                .memberId(playerInfo.getMemberId())
                .nickname(playerInfo.getNickname())
                .markerImageUrl(playerInfo.getMarkerImageUrl())
                .isHost(playerInfo.isHost())
                .build();
    }

    /**
     * 호스트 여부 설정
     * @param isHost 호스트 여부
     * @return 자기 자신 (메서드 체이닝을 위해)
     */
    public GameRoomPlayerInfo withHost(boolean isHost) {
        this.isHost = isHost;
        return this;
    }

} 