package com.kospot.exception.object.domain;

import com.kospot.exception.object.general.GeneralException;
import com.kospot.exception.payload.code.BaseCode;

public class NoticeHandler extends GeneralException {
    public NoticeHandler(BaseCode code) {
        super(code);
    }
}
