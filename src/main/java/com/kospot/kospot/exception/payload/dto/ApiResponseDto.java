package com.kospot.kospot.exception.payload.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.kospot.kospot.exception.payload.code.BaseCode;
import com.kospot.kospot.exception.payload.code.SuccessStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponseDto<T> {
    private final Boolean isSuccess;
    private final Integer code;
    private final String message;
    private T result;

    public static <T> ApiResponseDto<T> onSuccess(T result){
        return new ApiResponseDto(true, SuccessStatus._SUCCESS.getCode(),
                SuccessStatus._SUCCESS.getMessage(), result);
    }

    // 다양한 성공 상태 처리
    public static <T> ApiResponseDto<T> of(Integer code, String message, T result){
        return new ApiResponseDto<>(true, code,
                message, result);
    }

    public static <T> ApiResponseDto<T> onFailure(Integer code, String message, T data){
        return new ApiResponseDto<>(false, code, message, data);
    }

}