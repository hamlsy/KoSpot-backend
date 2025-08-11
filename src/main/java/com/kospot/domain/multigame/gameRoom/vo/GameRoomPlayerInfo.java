package com.kospot.domain.multigame.gameRoom.vo;

import com.kospot.domain.member.entity.Member;
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

    private boolean isHost;

    private Long joinedAt;

    //todo add player statistic
    
    /**
     * Member 엔티티로부터 GameRoomPlayerInfo 생성
     * @param member 멤버 엔티티
     * @return GameRoomPlayerInfo 객체
     */
    public static GameRoomPlayerInfo from(Member member) {
        return GameRoomPlayerInfo.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .markerImageUrl(member.getEquippedMarkerImage().getImageUrl())
                .isHost(false) // 호스트 여부는 별도로 설정 필요
                .joinedAt(System.currentTimeMillis())
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