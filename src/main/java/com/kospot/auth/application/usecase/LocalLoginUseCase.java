package com.kospot.auth.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.domain.exception.MemberErrorStatus;
import com.kospot.member.domain.exception.MemberHandler;
import com.kospot.member.domain.vo.AuthProvider;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.security.dto.JwtToken;
import com.kospot.common.security.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LocalLoginUseCase {

    private final MemberAdaptor memberAdaptor;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder;

    public JwtToken execute(String email, String rawPassword) {
        Member member = memberAdaptor.queryByEmail(email);

        if (member.getAuthProvider() != AuthProvider.LOCAL) {
            throw new MemberHandler(MemberErrorStatus.SOCIAL_ACCOUNT_ONLY);
        }

        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new MemberHandler(MemberErrorStatus.INVALID_PASSWORD);
        }

        return tokenService.generateTokenByMember(member);
    }
}
