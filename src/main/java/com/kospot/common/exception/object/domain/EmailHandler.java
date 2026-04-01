package com.kospot.common.exception.object.domain;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class EmailHandler extends GeneralException {
    public EmailHandler(BaseCode code) {
        super(code);
    }
}
