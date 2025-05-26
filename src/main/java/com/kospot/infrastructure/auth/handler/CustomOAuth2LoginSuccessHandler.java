package com.kospot.infrastructure.auth.handler;

import com.kospot.infrastructure.security.dto.JwtToken;
import com.kospot.infrastructure.security.service.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final String REDIRECT_URI = "localhost:8080/oauth2/callback";
    private final TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }
        log.info("--------------------------- OAuth2LoginSuccessHandler ---------------------------");
        JwtToken jwtToken = tokenService.generateToken(authentication);
        String provider = null;

        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            provider = oauth2Token.getAuthorizedClientRegistrationId();
            Collection<GrantedAuthority> authorities = oauth2Token.getAuthorities();
            authorities.forEach(grantedAuthority -> log.info("role {}", grantedAuthority.getAuthority()));
        }

        String url = UriComponentsBuilder.fromHttpUrl(REDIRECT_URI)
                .queryParam("code", jwtToken.getAccessToken())
                .queryParam("provider", provider) //todo refresh token
                .build()
                .toUriString();
        response.addHeader("Authorization",
                jwtToken.getGrantType() + " " + jwtToken.getAccessToken());

        response.sendRedirect(url);

    }
}