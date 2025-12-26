package com.kospot.infrastructure.adsense.aop;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.vo.Role;
import com.kospot.infrastructure.annotation.adsense.BotSuccess;
import com.kospot.infrastructure.exception.payload.code.Reason;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.dto.JwtToken;
import com.kospot.infrastructure.security.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class BotSuccessAspect {

    private final MemberAdaptor memberAdaptor;
    private final TokenService tokenService;

    @Around("@annotation(botSuccess)")
    public Object handleBotSuccess(ProceedingJoinPoint joinPoint, BotSuccess botSuccess) throws Throwable {
        // ✅ @CurrentMemberOrNull 파라미터만 확인 (Resolver가 이미 처리함)
        Member member = extractMemberFromMethodArgs(joinPoint);

        if (member != null && isBotMember(member)) {
            log.debug("BOT 회원 감지 → 가짜 성공 응답: {}", joinPoint.getSignature());
            return createFakeApiResponseDto(joinPoint);
        }

        return joinPoint.proceed();
    }

    /**
     * 핵심: @CurrentMember(OrNull) 파라미터만 추출
     * ArgumentResolver가 이미 Header/Cookie 처리 완료
     */
    private Member extractMemberFromMethodArgs(ProceedingJoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Member) {
                return (Member) arg;
            }
        }
        return null;
    }

    private Member getCurrentMember(ProceedingJoinPoint joinPoint) {
        NativeWebRequest request = getNativeWebRequest(joinPoint);
        String token = extractTokenFromCookies(request);
        Long memberId = tokenService.getMemberIdFromToken(token);
        return memberAdaptor.queryById(memberId);
    }

    private NativeWebRequest getNativeWebRequest(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof NativeWebRequest) {
                return (NativeWebRequest) arg;
            }
        }
        return null;
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

    private boolean isBotMember(Member member) {
        return member != null && Role.BOT.equals(member.getRole());
    }

    private ApiResponseDto<?> createFakeApiResponseDto(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> returnType = method.getReturnType();

        // ApiResponseDto<T> 타입 추출
        Class<?> genericType = getApiResponseGenericType(method);
        Object fakeResult = createDummyObject(genericType);

        // ApiResponseDto.onSuccess()으로 성공 응답
        return ApiResponseDto.onSuccess(fakeResult);
    }

    private Class<?> getApiResponseGenericType(Method method) {
        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericReturnType;
            if (pType.getRawType() == ApiResponseDto.class) {
                Type[] actualTypes = pType.getActualTypeArguments();
                if (actualTypes.length > 0) {
                    return (Class<?>) actualTypes[0];
                }
            }
        }
        return Object.class; // 기본 fallback
    }

    private Object createDummyObject(Class<?> resultType) {
        if (resultType == null || resultType == Void.TYPE || resultType == Object.class) {
            return "크롤러 테스트 데이터";
        }

        // 1. 기본 타입
        if (isPrimitiveOrWrapper(resultType)) {
            return createPrimitiveDummy(resultType);
        }

        // 2. String
        if (resultType == String.class) {
            return "크롤러 테스트 데이터";
        }

        // 3. Number
        if (Number.class.isAssignableFrom(resultType)) {
            return createNumberDummy(resultType);
        }

        // 4. 날짜
        if (isDateType(resultType)) {
            return LocalDateTime.now().minusDays(1);
        }

        // 5. Boolean
        if (resultType == Boolean.class || resultType == boolean.class) {
            return true;
        }

        // 6. Enum
        if (resultType.isEnum()) {
            return resultType.getEnumConstants()[0];
        }

        // 7. SuccessStatus (Reason 구현체)
        if (Reason.class.isAssignableFrom(resultType)) {
            try {
                Method successMethod = resultType.getMethod("getInstance");
                return successMethod.invoke(null);
            } catch (Exception e) {
                return SuccessStatus._SUCCESS; // 기본 fallback
            }
        }

        // 8. DTO 객체 (Builder 패턴)
        return createDtoDummy(resultType);
    }

    private Object createPrimitiveDummy(Class<?> type) {
        if (type == int.class || type == Integer.class) return 1;
        if (type == long.class || type == Long.class) return 999L;
        if (type == double.class || type == Double.class) return 100.0;
        return 0;
    }

    private Object createNumberDummy(Class<?> type) {
        if (type == Long.class) return 999L;
        if (type == Integer.class) return 100;
        return 0L;
    }

    private boolean isDateType(Class<?> type) {
        return LocalDateTime.class.isAssignableFrom(type) ||
                LocalDate.class.isAssignableFrom(type);
    }

    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                Number.class.isAssignableFrom(type) ||
                type == Boolean.class;
    }

    /**
     * DTO 더미 객체 (Builder 패턴 우선)
     */
    private Object createDtoDummy(Class<?> dtoClass) {
        try {
            // @Builder가 있는 경우
            Method builderMethod = Arrays.stream(dtoClass.getMethods())
                    .filter(m -> m.getName().equals("builder"))
                    .findFirst()
                    .orElse(null);

            if (builderMethod != null) {
                Object builder = builderMethod.invoke(null);
                setDummyBuilderFields(builder);
                Method buildMethod = dtoClass.getMethod("build");
                return buildMethod.invoke(builder);
            }

            // 기본 생성자 + setter
            Object instance = dtoClass.getDeclaredConstructor().newInstance();
            setDummyDtoFields(instance);
            return instance;

        } catch (Exception e) {
            log.warn("DTO 더미 생성 실패 {}: {}", dtoClass.getSimpleName(), e.getMessage());
            return java.util.Map.of("id", 999L, "name", "크롤러 테스트");
        }
    }

    private void setDummyBuilderFields(Object builder) throws Exception {
        // id 필드 설정
        Method setId = Arrays.stream(builder.getClass().getMethods())
                .filter(m -> m.getName().equals("id") && m.getParameterCount() == 1)
                .findFirst()
                .orElse(null);
        if (setId != null) {
            setId.invoke(builder, 999L);
        }
    }

    private void setDummyDtoFields(Object instance) throws Exception {
        Method setId = Arrays.stream(instance.getClass().getMethods())
                .filter(m -> m.getName().equals("setId"))
                .findFirst()
                .orElse(null);
        if (setId != null) {
            setId.invoke(instance, 999L);
        }
    }

}
