package com.kospot.application.admin.gameconfig;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gameconfig.entity.GameConfig;
import com.kospot.domain.gameconfig.service.GameConfigService;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.request.AdminGameConfigRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateGameConfigUseCase {

    private final GameConfigService gameConfigService;

    @Transactional
    public Long execute(Member admin, AdminGameConfigRequest.Create request) {
        admin.validateAdmin();

        GameMode gameMode = GameMode.fromKey(request.getGameModeKey());
        PlayerMatchType playerMatchType = request.getPlayerMatchTypeKey() != null
                ? PlayerMatchType.fromKey(request.getPlayerMatchTypeKey())
                : null;

        GameConfig gameConfig = GameConfig.builder()
                .gameMode(gameMode)
                .playerMatchType(playerMatchType)
                .isSingleMode(request.getIsSingleMode())
                .isActive(true)
                .build();

        GameConfig saved = gameConfigService.createGameConfig(gameConfig);

        return saved.getId();
    }
}

