package com.kospot.application.admin.gameconfig;

import com.kospot.domain.gameconfig.adaptor.GameConfigAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.response.AdminGameConfigResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class FindAllGameConfigsUseCase {

    private final GameConfigAdaptor gameConfigAdaptor;

    public List<AdminGameConfigResponse.GameConfigInfo> execute(Member admin) {
        admin.validateAdmin();

        return gameConfigAdaptor.queryAll()
                .stream()
                .map(AdminGameConfigResponse.GameConfigInfo::from)
                .collect(Collectors.toList());
    }
}

