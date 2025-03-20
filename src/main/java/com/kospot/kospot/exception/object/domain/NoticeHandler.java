package com.kospot.kospot.exception.object.domain;

import com.kospot.kospot.exception.object.general.GeneralException;
import com.kospot.kospot.exception.payload.code.BaseCode;

public class NoticeHandler extends GeneralException {
    public NoticeHandler(BaseCode code) {
        super(code);
    }
}
