package com.kospot.common.exception.object.general;

import com.kospot.common.exception.payload.code.BaseCode;
import com.kospot.common.exception.payload.code.Reason;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException{
    private BaseCode code;

    public Reason getErrorReason(){
        return this.code.getReason();
    }

    public Reason getErrorReasonHttpStatus(){
        return this.code.getReasonHttpStatus();
    }
}
