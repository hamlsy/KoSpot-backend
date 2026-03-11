package com.kospot.common.exception.object.domain;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class NotificationHandler extends GeneralException {
    public NotificationHandler(BaseCode code) {
        super(code);
    }
}
