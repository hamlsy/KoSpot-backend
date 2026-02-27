package com.kospot.domain.friend.exception;

import com.kospot.infrastructure.exception.payload.code.BaseCode;
import com.kospot.infrastructure.exception.payload.code.Reason;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum FriendErrorStatus implements BaseCode {

    FRIEND_REQUEST_NOT_FOUND(NOT_FOUND, 4150, "친구 요청을 찾을 수 없습니다."),
    FRIENDSHIP_NOT_FOUND(NOT_FOUND, 4151, "친구 관계를 찾을 수 없습니다."),
    FRIEND_CHAT_ROOM_NOT_FOUND(NOT_FOUND, 4152, "친구 채팅방을 찾을 수 없습니다."),

    CANNOT_REQUEST_SELF(BAD_REQUEST, 4153, "자기 자신에게 친구 요청을 보낼 수 없습니다."),
    FRIEND_REQUEST_ALREADY_PENDING(CONFLICT, 4154, "이미 대기 중인 친구 요청이 있습니다."),
    ALREADY_FRIENDS(CONFLICT, 4155, "이미 친구 관계입니다."),
    INVALID_FRIEND_REQUEST_STATE(CONFLICT, 4156, "친구 요청 상태가 올바르지 않습니다."),

    FRIEND_REQUEST_RECEIVER_ONLY(FORBIDDEN, 4157, "요청 수신자만 처리할 수 있습니다."),
    FRIEND_CHAT_ACCESS_DENIED(FORBIDDEN, 4158, "해당 친구 채팅방에 접근할 수 없습니다.");

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
