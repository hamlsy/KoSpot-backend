package com.kospot.infrastructure.exception.object.domain;

import com.kospot.infrastructure.exception.object.general.GeneralException;
import com.kospot.infrastructure.exception.payload.code.BaseCode;

public class NoticeHandler extends GeneralException {
    public NoticeHandler(BaseCode code) {
        super(code);
    }
}
