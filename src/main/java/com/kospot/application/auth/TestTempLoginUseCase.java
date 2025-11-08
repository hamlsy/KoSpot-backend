package com.kospot.application.auth;

import com.kospot.application.member.RegisterSocialMemberUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.point.service.PointService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.security.dto.JwtToken;
import com.kospot.infrastructure.security.service.TokenService;
import com.kospot.infrastructure.security.vo.CustomUserDetails;
import com.kospot.presentation.auth.dto.response.AuthResponse;
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
