package com.kospot.application.admin.adsense;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.dto.JwtToken;
import com.kospot.infrastructure.security.service.TokenService;
import com.kospot.infrastructure.security.vo.CustomUserDetails;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LoginAdsenseUseCase {

    private static final String ADSENSE_BOT_USER_ID = "adsense_bot";
    private static final String ADSENSE_BOT_PASSWORD = "fixed_password123";

    private final MemberAdaptor memberAdaptor;
    private final TokenService tokenService;

    public JwtToken execute(String username, String password) {
        if (!ADSENSE_BOT_USER_ID.equals(username) || !ADSENSE_BOT_PASSWORD.equals(password)) {
            throw new IllegalArgumentException("애드센스 로그인 실패");
        }

        // 더미용 멤버 조회
        Member member = memberAdaptor.queryFirstBotMember();
        return generateToken(member);
    }

    private JwtToken generateToken(Member member) {
        CustomUserDetails userDetails = CustomUserDetails.from(member);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                List.of(new SimpleGrantedAuthority(
                        "ROLE_BOT"))
        );
        return tokenService.generateToken(auth);
    }
}
