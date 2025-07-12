package com.kospot.infrastructure.exception.payload.code;

public interface BaseCode {
    Reason getReason();
    Reason getReasonHttpStatus();
}
