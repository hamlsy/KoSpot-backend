package com.kospot.application.admin.gameconfig;

import com.kospot.domain.gameconfig.adaptor.GameConfigAdaptor;
import com.kospot.domain.gameconfig.entity.GameConfig;
import com.kospot.domain.gameconfig.service.GameConfigService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ActivateGameConfigUseCase {

    private final GameConfigAdaptor gameConfigAdaptor;
    private final GameConfigService gameConfigService;

    @Transactional
    public void execute(Member admin, Long configId) {
        admin.validateAdmin();
        
        GameConfig config = gameConfigAdaptor.queryById(configId);
        gameConfigService.activateGameConfig(config);
    }
}

