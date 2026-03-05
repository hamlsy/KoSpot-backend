package com.kospot.infrastructure.security.resolver;

import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.infrastructure.security.aop.CurrentMemberOrNull;
import com.kospot.infrastructure.security.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
public final class CustomAuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasCurrentMember = parameter.hasParameterAnnotation(CurrentMember.class);
        boolean hasCurrentMemberOrNull = parameter.hasParameterAnnotation(CurrentMemberOrNull.class);
        boolean isLongType = Long.class.isAssignableFrom(parameter.getParameterType());

        if ((hasCurrentMember || hasCurrentMemberOrNull) && !isLongType) {
            throw new IllegalStateException(
                    "@CurrentMember / @CurrentMemberOrNull 은 Long 타입 파라미터에만 사용할 수 있습니다. 문제 파라미터: "
                            + parameter.getDeclaringClass().getSimpleName() + "#" + parameter.getMethod().getName());
        }

        return isLongType && (hasCurrentMember || hasCurrentMemberOrNull);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        boolean nullable = parameter.hasParameterAnnotation(CurrentMemberOrNull.class);
        Long memberId = extractMemberIdFromAuthentication(SecurityContextHolder.getContext().getAuthentication());

        if (memberId == null) {
            if (nullable) return null;
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        return memberId;
    }

    private Long extractMemberIdFromAuthentication(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        String name = authentication.getName();
        if (!StringUtils.hasText(name) || "anonymousUser".equals(name)) {
            return null;
        }

        try {
            return Long.parseLong(name);
        } catch (Exception e) {
            log.warn("Authentication name을 memberId로 파싱 실패: name={}", name);
            return null;
        }
    }
}
