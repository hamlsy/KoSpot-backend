package com.kospot.auth.application.usecase;

import com.kospot.member.application.usecase.RegisterSocialMemberUseCase;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.infrastructure.persistence.MemberRepository;
import com.kospot.point.application.service.PointService;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.security.dto.JwtToken;
import com.kospot.common.security.service.TokenService;
import com.kospot.common.security.vo.CustomUserDetails;
import com.kospot.auth.presentation.dto.response.AuthResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class TestTempLoginUseCase {

    private final MemberRepository memberRepository;
    private final TokenService tokenService;
    private final PointService pointService;
    private final RegisterSocialMemberUseCase registerSocialMemberUseCase;

    public AuthResponse.TempLogin testLogin(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseGet(() -> {
                    Member newMember = registerSocialMemberUseCase.execute(username, "kospot@email");
                    pointService.addPoint(newMember, 100000);
                    return newMember;
                });

        CustomUserDetails userDetails = CustomUserDetails.from(member);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(

                userDetails,
                null,
                List.of(new SimpleGrantedAuthority(
                        "ROLE_USER"))
        );

        JwtToken jwtToken = tokenService.generateToken(auth);

        return AuthResponse.TempLogin.from(jwtToken, member.getId());
    }

}
