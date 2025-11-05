package com.kospot.infrastructure.security.resolver;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.infrastructure.security.aop.CurrentMemberOrNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.annotation.Annotation;

@Slf4j
@Component
@RequiredArgsConstructor
public final class CustomAuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

    private final MemberAdaptor memberAdaptor;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return findMethodAnnotation(CurrentMember.class, parameter) != null
                || findMethodAnnotation(CurrentMemberOrNull.class, parameter) != null;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (authentication == null || authentication.getPrincipal() == null || principal.equals("anonymousUser")) {
            // nullable 버전이면 null 리턴
            if (hasAnnotation(parameter, CurrentMemberOrNull.class)) {
                return null;
            }
            // 필수 버전이면 예외
            throw new RuntimeException("로그인이 필요합니다.");
        }

        CurrentMember annotation = findMethodAnnotation(CurrentMember.class, parameter);
        findMethodAnnotation(CurrentMember.class, parameter);
        return memberAdaptor.queryById(Long.parseLong(authentication.getName()));
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
}