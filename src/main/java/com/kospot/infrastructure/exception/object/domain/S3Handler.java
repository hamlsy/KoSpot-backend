package com.kospot.infrastructure.exception.object.domain;

import com.kospot.infrastructure.exception.object.general.GeneralException;
import com.kospot.infrastructure.exception.payload.code.BaseCode;

public class S3Handler extends GeneralException {
    public S3Handler(BaseCode code) {
        super(code);
    }
}
