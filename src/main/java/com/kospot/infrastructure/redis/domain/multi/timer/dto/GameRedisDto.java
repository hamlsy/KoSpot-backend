package com.kospot.infrastructure.redis.domain.multi.timer.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class GameRedisDto {

    private Long gameId;            // MultiRoadViewGame / MultiPhotoGame PK
    private String gameType;        // "ROADVIEW" / "PHOTO"
    private Integer currentRound;   // 현재 라운드 번호
    private Integer totalRounds;    // 전체 라운드 수
    private Set<String> playerIds;  // 참여자 ID 집합
    private Long startTimeMs;       // 라운드 시작 시간 (서버 기준)
    private Long durationMs;        // 라운드 지속 시간

}
