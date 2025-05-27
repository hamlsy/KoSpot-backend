package com.kospot.global.exception.object.domain;

import com.kospot.global.exception.object.general.GeneralException;
import com.kospot.global.exception.payload.code.BaseCode;

public class S3Handler extends GeneralException {
    public S3Handler(BaseCode code) {
        super(code);
    }
}
