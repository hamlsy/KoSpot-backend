package com.kospot.common.exception.object.domain;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class EventHandler extends GeneralException {
    public EventHandler(BaseCode code) {
        super(code);
    }
}
