package com.kospot.infrastructure.exception.payload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseCode {
    //server
    _INTERNAL_SERVER_ERROR(INTERNAL_SERVER_ERROR, 5000, "서버 에러"),

    // general error
    _BAD_REQUEST(BAD_REQUEST, 4000, "잘못된 요청입니다."),
    _UNAUTHORIZED(UNAUTHORIZED, 4001, "로그인이 필요합니다."),
    _FORBIDDEN(FORBIDDEN, 4002, "금지된 요청입니다."),

    //auth error(4050 ~ 4099)
    AUTH_INVALID_REFRESH_TOKEN(UNAUTHORIZED, 4050, "유효하지 않은 리프레시 토큰입니다."),
    AUTH_INVALID_TOKEN(UNAUTHORIZED, 4051, "유효하지 않은 액세스 토큰입니다"),
    AUTH_TOKEN_HAS_EXPIRED(UNAUTHORIZED, 4052, "토큰의 유효기간이 만료되었습니다"),
    AUTH_TOKEN_IS_UNSUPPORTED(UNAUTHORIZED, 4053, "토큰 형식이 jwt와는 다른 형식입니다."),
    AUTH_IS_NULL(UNAUTHORIZED, 4054, "토큰이 null입니다"),
    AUTH_MUST_AUTHORIZED_URI(BAD_REQUEST, 4055, "인증이 필수인 uri입니다."),
    AUTH_ROLE_CANNOT_EXECUTE_URI(BAD_REQUEST, 4056, "해당 인가로는 실행할 수 없는 동작입니다."),
    AUTH_INVALID_AUTHENTICATION(UNAUTHORIZED, 4057, "유효하지 않은 인증 객체입니다."),
    AUTH_ADMIN_PRIVILEGES_REQUIRED(FORBIDDEN, 4003, "관리자의 권한이 필요합니다."),

    //member error(4100 ~ 4149)
    MEMBER_NOT_FOUND(NOT_FOUND, 4100, "찾을 수 없는 유저 정보입니다."),

    //coordinate error(4150 ~ 4199)
    COORDINATE_NOT_FOUND(NOT_FOUND, 4150, "해당 좌표를 찾을 수 없습니다."),
    SIDO_NOT_FOUND(NOT_FOUND, 4151, "해당 시도를 찾을 수 없습니다."),
    COORDINATE_CACHE_TABLE_ID_NOT_FOUND(NOT_FOUND, 4152, "해당 좌표 캐시 테이블 ID를 찾을 수 없습니다."),

    // coordinate repository factory error(4200)
    DYNAMIC_COORDINATE_REPOSITORY_FACTORY_NOT_FOUND(NOT_FOUND, 4200, "해당 시도에 대한 레포지토리를 찾을 수 없습니다."),

    // File Error (4201 ~ 4210)
    FILE_READ_ERROR(INTERNAL_SERVER_ERROR, 4201, "파일을 읽는 중 오류가 발생했습니다."),
    FILE_NOT_FOUND(NOT_FOUND, 4202, "파일을 찾을 수 없습니다."),
    FILE_INVALID_EXTENSION(BAD_REQUEST, 4203, "잘못된 형식의 파일입니다."),
    FILE_EXTENSION_NOT_FOUND(NOT_FOUND, 4204, "파일의 형식을 찾을 수 없습니다."),
    FILE_UPLOAD_FAILED(INTERNAL_SERVER_ERROR, 4205, "파일 업로드에 실패했습니다."),

    // Game Error (4211 ~ 4250)
    GAME_NOT_FOUND(NOT_FOUND, 4211, "해당 게임을 찾을 수 없습니다."),
    GAME_IS_ALREADY_COMPLETED(BAD_REQUEST, 4212, "이미 처리된 게임입니다."),
    GAME_COORDINATES_ENCRYPT_ERROR(BAD_REQUEST, 4213, "게임 좌표 암호화 중 오류가 발생했습니다."),
    GAME_TYPE_NOT_FOUND(NOT_FOUND, 4214, "존재하지 않는 게임입니다."),
    GAME_MODE_NOT_FOUND(NOT_FOUND, 4215, "존재하지 않는 게임 모드입니다."),
    GAME_NOT_SAME_MEMBER(BAD_REQUEST, 4216, "동일한 사용자가 아닙니다."),

    // Point Error (4251 ~ 4260)
    POINT_INSUFFICIENT(BAD_REQUEST, 4251, "포인트가 부족합니다."),

    // Event Error (4261 ~ 4270)
    EVENT_GAME_END_ERROR(INTERNAL_SERVER_ERROR, 4261, "게임 종료 중 오류가 발생했습니다."),

    // Item Error (4271 ~ 4280)
    ITEM_NOT_FOUND(NOT_FOUND, 4271, "해당 아이템을 찾을 수 없습니다."),
    ITEM_OUT_OF_STOCK(BAD_REQUEST, 4272, "아이템 재고가 없습니다."),

    // Image Error (4281 ~ 4290)
    IMAGE_TYPE_NOT_FOUND(NOT_FOUND, 4281, "해당 이미지 타입을 찾을 수 없습니다."),
    IMAGE_NOT_FOUND(NOT_FOUND, 4282, "해당 이미지를 찾을 수 없습니다."),

    // Notice Error(4291 ~ 4300)
    NOTICE_NOT_FOUND(NOT_FOUND, 4291, "해당 공지사항을 찾을 수 없습니다."),

    // GameRoom Error(4301 ~ 4310)
    GAME_ROOM_NOT_FOUND(NOT_FOUND, 4301, "게임 방을 찾을 수 없습니다."),
    GAME_ROOM_IS_FULL(BAD_REQUEST, 4302, "정원이 부족합니다."),
    GAME_ROOM_IS_NOT_CORRECT_PASSWORD(BAD_REQUEST, 4303, "틀린 비밀번호 입니다."),
    GAME_ROOM_IS_ALREADY_IN_PROGRESS(BAD_REQUEST, 4304, "게임이 이미 진행 중입니다."),
    GAME_ROOM_HOST_PRIVILEGES_REQUIRED(FORBIDDEN, 4305, "방장 권한이 필요합니다."),
    GAME_ROOM_MEMBER_ALREADY_IN_ROOM(BAD_REQUEST, 4306, "이미 게임 방에 참여 중입니다."),
    GAME_ROOM_IS_NOT_ENOUGH_PLAYER(BAD_REQUEST, 4307, "게임 방에 참여할 플레이어가 부족합니다."),
    GAME_ROOM_CANNOT_JOIN_NOW(BAD_REQUEST, 4308, "현재 게임 방에 참여할 수 없습니다."),
    GAME_ROOM_PLAYER_NOT_FOUND(NOT_FOUND, 4309, "해당 게임 방 플레이어를 찾을 수 없습니다."),

    // MultiGame Error(4311 ~ 4320)
    PLAYER_MATCH_TYPE_NOT_FOUND(NOT_FOUND, 4311, "해당 플레이어 매치 타입을 찾을 수 없습니다."),

    // Game Round Error(4321 ~ 4330)
    GAME_ROUND_NOT_FOUND(NOT_FOUND, 4321, "해당 게임 라운드를 찾을 수 없습니다."),
    ROUND_ALREADY_FINISHED(BAD_REQUEST, 4322, "이미 종료된 라운드입니다."),
    ROUND_ALREADY_SUBMITTED(BAD_REQUEST, 4323, "해당 라운드에는 이미 제출했습니다."),

    // Game Player Error(4331 ~ 4340)
    GAME_PLAYER_NOT_FOUND(NOT_FOUND, 4331, "해당 게임 플레이어를 찾을 수 없습니다."),

    // Chat Error(4341 ~ 4350)
    CHAT_RATE_LIMIT_EXCEEDED(BAD_REQUEST, 4341, "채팅 속도 제한을 초과했습니다."),
    CHAT_MESSAGE_CONTENT_EMPTY(BAD_REQUEST, 4342, "채팅 메시지 내용이 비어있습니다."),
    CHAT_INVALID_CHANNEL_TYPE(BAD_REQUEST, 4343, "유효하지 않은 채널 타입입니다."),

    // WebSocket Channel Error (4351 ~ 4360)
    INVALID_DESTINATION(BAD_REQUEST, 4351, "유효하지 않은 구독 목적지입니다."),

    // Banner Error (4361 ~ 4370)
    BANNER_NOT_FOUND(NOT_FOUND, 4361, "해당 배너를 찾을 수 없습니다."),

    // GameConfig Error (4371 ~ 4380)
    GAME_CONFIG_NOT_FOUND(NOT_FOUND, 4371, "게임 설정을 찾을 수 없습니다."),


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
    //server error


}
