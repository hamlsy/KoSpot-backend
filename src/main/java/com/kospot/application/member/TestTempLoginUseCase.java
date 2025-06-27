package com.kospot.application.member;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.security.dto.JwtToken;
import com.kospot.infrastructure.security.service.TokenService;
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

    public JwtToken testLogin(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseGet(() -> memberRepository.save(createTemporary(username)));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                member.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        return tokenService.generateToken(auth);
    }

    private Member createTemporary(String username){
        return Member.builder()
                .username(username)
                .nickname(username)
                .role(Role.USER)
                .point(100000)
                .build();
    }

}
