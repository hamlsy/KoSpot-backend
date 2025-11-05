package com.kospot.application.member;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckNicknameDuplicateUseCase {

    private final MemberAdaptor memberAdaptor;

    public boolean execute(String nickname) {
        return memberAdaptor.existsByNickname(nickname);
    }

}
