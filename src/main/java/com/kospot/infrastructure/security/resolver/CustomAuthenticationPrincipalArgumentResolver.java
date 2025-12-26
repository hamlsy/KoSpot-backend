package com.kospot.infrastructure.security.resolver;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.infrastructure.security.aop.CurrentMemberOrNull;
import com.kospot.infrastructure.security.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.annotation.Annotation;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public final class CustomAuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

    private final MemberAdaptor memberAdaptor;
    private final TokenService tokenService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return findMethodAnnotation(CurrentMember.class, parameter) != null
                || findMethodAnnotation(CurrentMemberOrNull.class, parameter) != null;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        boolean nullable = hasAnnotation(parameter, CurrentMemberOrNull.class);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        Long memberId = extractMemberIdFromAuthentication(authentication);
        if (memberId == null) {
            String token = extractTokenFromCookies(webRequest);
            if (token != null) {
                try {
                    memberId = tokenService.getMemberIdFromToken(token);
                    log.debug("쿠키 JWT로 memberId 복원 성공: memberId={}", memberId);
                } catch (Exception e) {
                    log.debug("쿠키 JWT 파싱 실패(무시): {}", e.getMessage());
                    memberId = null;
                }
            }
        }
        if (memberId == null) {
            if (nullable) return null;
            throw new RuntimeException("로그인이 필요합니다.");
        }

        return memberAdaptor.queryById(memberId);
    }

    private <T extends Annotation> T findMethodAnnotation(Class<T> annotationClass, MethodParameter parameter) {
        T annotation = parameter.getParameterAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }

        Annotation[] annotations = parameter.getParameterAnnotations();
        for (Annotation a : annotations) {
            if (a.annotationType().isAnnotationPresent(annotationClass)) {
                return parameter.getParameterAnnotation(annotationClass);
            }
        }

        return null;
    }
    private boolean hasAnnotation(MethodParameter parameter, Class<? extends Annotation> annotationClass) {
        return parameter.getParameterAnnotation(annotationClass) != null;
    }

    private Long extractMemberIdFromAuthentication(Authentication authentication) {
        if (authentication == null) return null;
        if (!authentication.isAuthenticated()) return null;
        if (authentication instanceof AnonymousAuthenticationToken) return null;
        Object principal = authentication.getPrincipal();
        if (principal == null) return null;
        if (Objects.equals(principal, "anonymousUser")) return null;
        try {
            return Long.parseLong(authentication.getName());
        } catch (Exception e) {
            log.debug("Authentication name을 memberId로 파싱 실패: name={}", authentication.getName());
            return null;
        }
    }

    private String extractTokenFromCookies(NativeWebRequest webRequest) {
        // 애드센스 크롤러용 쿠키명들 (JSESSIONID, accessToken, authToken 등)
        String[] cookieNames = {"accessToken", "authToken", "JWT", "JSESSIONID"};

        for (String cookieName : cookieNames) {
            String cookieValue = webRequest.getHeader("Cookie");
            if (cookieValue != null) {
                // 쿠키 파싱 (간단한 버전)
                String[] cookies = cookieValue.split(";");
                for (String cookie : cookies) {
                    String[] parts = cookie.trim().split("=");
                    if (parts.length == 2 && parts[0].equals(cookieName)) {
                        String token = parts[1];
                        // JWT 형식인지 간단히 확인
                        if (token.startsWith("eyJ") && token.contains(".")) {
                            log.debug("쿠키에서 JWT 추출: {}={}", cookieName, token.substring(0, 20) + "...");
                            return token;
                        }
                    }
                }
            }
        }
        return null;
    }
}