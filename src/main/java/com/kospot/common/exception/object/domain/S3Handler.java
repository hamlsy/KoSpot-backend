package com.kospot.common.exception.object.domain;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class S3Handler extends GeneralException {
    public S3Handler(BaseCode code) {
        super(code);
    }
}
