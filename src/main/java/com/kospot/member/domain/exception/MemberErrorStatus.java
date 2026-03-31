package com.kospot.member.domain.exception;

import com.kospot.common.exception.payload.code.BaseCode;
import com.kospot.common.exception.payload.code.Reason;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum MemberErrorStatus implements BaseCode  {

    //member error(4100 ~ 4149)
    MEMBER_NOT_FOUND(NOT_FOUND, 4100, "찾을 수 없는 유저 정보입니다."),
    NICKNAME_ALREADY_EXISTS(NOT_FOUND, 4101, "이미 존재하는 닉네임입니다."),
    EMAIL_ALREADY_EXISTS(CONFLICT, 4102, "이미 사용 중인 이메일입니다."),
    EMAIL_NOT_FOUND(UNAUTHORIZED, 4103, "존재하지 않는 이메일입니다."),
    INVALID_PASSWORD(UNAUTHORIZED, 4104, "비밀번호가 일치하지 않습니다."),
    SOCIAL_ACCOUNT_ONLY(BAD_REQUEST, 4105, "소셜 계정 전용입니다. 소셜 로그인을 이용해주세요.");


    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;

    @Override
    public Reason getReason() {
        return Reason.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public Reason getReasonHttpStatus() {
        return Reason.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}
