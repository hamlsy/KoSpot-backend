package com.kospot.application.admin.gameconfig;

import com.kospot.domain.gameconfig.service.GameConfigService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class InitializeAllGameConfigsUseCase {

    private final GameConfigService gameConfigService;

    @Transactional
    public void execute(Member admin) {
        admin.validateAdmin();
        
        // 없는 기본 설정만 생성
        gameConfigService.ensureDefaultConfigsExist();
    }
}

