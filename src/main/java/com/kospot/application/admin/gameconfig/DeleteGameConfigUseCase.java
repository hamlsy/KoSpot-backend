package com.kospot.application.admin.gameconfig;

import com.kospot.domain.gameconfig.service.GameConfigService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteGameConfigUseCase {

    private final GameConfigService gameConfigService;

    @Transactional
    public void execute(Member admin, Long configId) {
        admin.validateAdmin();
        gameConfigService.deleteGameConfig(configId);
    }
}

