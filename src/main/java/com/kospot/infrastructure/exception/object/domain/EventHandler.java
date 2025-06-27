package com.kospot.infrastructure.exception.object.domain;

import com.kospot.infrastructure.exception.object.general.GeneralException;
import com.kospot.infrastructure.exception.payload.code.BaseCode;

public class EventHandler extends GeneralException {
    public EventHandler(BaseCode code) {
        super(code);
    }
}
