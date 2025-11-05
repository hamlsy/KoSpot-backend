package com.kospot.application.auth;

import com.kospot.domain.gamerank.service.GameRankService;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.service.MemberStatisticService;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.member.repository.MemberRepository;
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
    private final MemberStatisticService memberStatisticService;
    private final GameRankService gameRankService;

    public AuthResponse.TempLogin testLogin(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseGet(() -> {
                    Member newMember = memberRepository.save(createTemporary(username));
                    memberStatisticService.initializeStatistic(newMember);
                    gameRankService.initGameRank(newMember);
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

    private Member createTemporary(String username){
        return Member.builder()
                .username(username)
                .nickname(username)
                .role(Role.USER)
                .firstVisited(true)
                .point(100000)
                .build();
    }

}
