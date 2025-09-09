package com.kospot.domain.multigame.gamePlayer.exception;

import com.kospot.infrastructure.exception.payload.code.BaseCode;
import com.kospot.infrastructure.exception.payload.code.Reason;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum GameTeamErrorStatus implements BaseCode {

    // Game Team ERror (4361 ~ 4370)
    GAME_TEAM_NOT_FOUND(NOT_FOUND, 4361, "해당 게임 팀을 찾을 수 없습니다."),
    GAME_TEAM_CANNOT_JOIN_NOW(BAD_REQUEST, 4362, "현재 게임 팀에 참여할 수 없습니다."),
    GAME_TEAM_ALREADY_JOINED(BAD_REQUEST, 4363, "이미 해당 게임 팀에 참여 중입니다."),
    ;

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
