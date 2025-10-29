package com.kospot.presentation.admin.dto.response;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gameconfig.entity.GameConfig;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class AdminGameConfigResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GameConfigInfo {
        private Long configId;
        private GameMode gameMode;
        private PlayerMatchType playerMatchType;
        private Boolean isSingleMode;
        private Boolean isActive;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static GameConfigInfo from(GameConfig config) {
            String description = buildDescription(config);

            return GameConfigInfo.builder()
                    .configId(config.getId())
                    .gameMode(config.getGameMode())
                    .playerMatchType(config.getPlayerMatchType())
                    .isSingleMode(config.getIsSingleMode())
                    .isActive(config.getIsActive())
                    .description(description)
                    .createdAt(config.getCreatedDate())
                    .updatedAt(config.getLastModifiedDate())
                    .build();
        }

        private static String buildDescription(GameConfig config) {
            String modeType = config.getIsSingleMode() ? "싱글" : "멀티";
            String gameMode = config.getGameMode().getMode();
            String matchType = config.getPlayerMatchType() != null
                    ? " - " + config.getPlayerMatchType().getType()
                    : "";

            return String.format("%s %s%s", modeType, gameMode, matchType);
        }
    }
}

