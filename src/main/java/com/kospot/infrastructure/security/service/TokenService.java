package com.kospot.infrastructure.security.service;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.auth.domain.CustomOAuthUser;
import com.kospot.infrastructure.exception.object.general.GeneralException;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.security.dto.JwtToken;
import com.kospot.infrastructure.security.vo.CustomUserDetails;
import com.kospot.infrastructure.redis.common.service.RedisService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TokenService {
    private final Key key;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserDetailsService userDetailsService;
    private final MemberAdaptor memberAdaptor;
    private final RedisService redisService;

    private final static int ACCESS_TOKEN_EXPIRATION_TIME = 1800000;
    private final static int REFRESH_TOKEN_EXPIRATION_TIME = 604800000;

    public TokenService(@Value("${app.jwt.secret}") String key,
                        AuthenticationManagerBuilder authenticationManagerBuilder,
                        UserDetailsService userDetailsService, MemberAdaptor memberAdaptor,
                        RedisService redisService) {
        byte[] keyBytes = Decoders.BASE64.decode(key);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userDetailsService = userDetailsService;
        this.memberAdaptor = memberAdaptor;
        this.redisService = redisService;
    }

    public JwtToken issueTokens(String refreshToken) {
        // Refresh Token 유효성 검사
        if (!validateToken(refreshToken) || !existsRefreshToken(refreshToken)) {
            throw new GeneralException(ErrorStatus.AUTH_INVALID_REFRESH_TOKEN);
        }

        // 이전 리프레시 토큰 삭제
        redisService.deleteToken(refreshToken);

        // 새로운 Authentication 객체 생성
        Claims claims = parseClaims(refreshToken);
        String memberId = claims.getSubject();
        CustomUserDetails customUserDetails = CustomUserDetails.from(memberAdaptor.queryById(Long.parseLong(memberId)));
//        Member member = memberAdaptor.queryById(Long.parseLong(memberId));
//        CustomOAuthUser customUserDetails = CustomOAuthUser.from(member);
        Authentication authentication = new UsernamePasswordAuthenticationToken(customUserDetails, "",
                customUserDetails.getAuthorities());

        // 새 토큰 생성
        return generateToken(authentication);
    }

    public JwtToken generateToken(Authentication authentication) {
        // 권한 가져오기
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        // 추가 정보 추출
        Object principal = authentication.getPrincipal();
        Long memberId;
        String nickname;
        String email;
        String role;
//        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            memberId = cud.getMemberId();
            nickname = cud.getNickname();
            email = cud.getEmail();
            role = cud.getRole();
        } else if (principal instanceof CustomOAuthUser oau) {
            memberId = oau.getMember().getId();
            nickname = oau.getMember().getNickname();
            email = oau.getMember().getEmail();
            role = oau.getMember().getRole().name();
        } else {
            throw new IllegalStateException("Unsupported principal: " + principal);
        }

        // Access Token 생성
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRATION_TIME);   // 30분
        log.info("date = {}", accessTokenExpiresIn);
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName()) // = customUserDetails.getUsername()
                .claim("auth", authorities)
                .claim("memberId", memberId)
                .claim("nickname", nickname)
                .claim("email", email)
                .claim("role", role)
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token 생성
        String refreshToken = Jwts.builder()
                .setSubject(authentication.getName())
                .setExpiration(new Date(now + REFRESH_TOKEN_EXPIRATION_TIME))    // 7일
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // 새 리프레시 토큰을 Redis에 저장
        redisService.setToken(refreshToken, authentication.getName());

        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(String refreshToken) {
        redisService.deleteToken(refreshToken);
    }

    public boolean existsRefreshToken(String refreshToken) {
        return redisService.getToken(refreshToken) != null;
    }

    public Authentication getAuthentication(String accessToken) {
        // Jwt 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities = Arrays
                .stream(claims.get("auth").toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // UserDetails 객체를 만들어서 Authentication return
        String username = claims.getSubject();
        UserDetails principal = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            token = removeBearerHeader(token);
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
            throw new IllegalStateException("Invalid JWT Token");
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
            throw new IllegalStateException("Expired JWT Token");
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
            throw new IllegalStateException("Unsupported JWT Token");
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
            throw new IllegalArgumentException("JWT claims string is empty.");
        }
    }

    // remove bearer header method
    public String removeBearerHeader(String token) {
        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }


    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // JWT에서 memberId 추출
    public Long getMemberIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("memberId", Long.class);
    }

    // JWT에서 nickname 추출
    public String getNicknameFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("nickname", String.class);
    }

    // JWT에서 email 추출
    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("email", String.class);
    }

    // JWT에서 role 추출 (String으로 반환)
    public String getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    // JWT에서 권한 정보 추출
    public String getAuthoritiesFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("auth", String.class);
    }


}