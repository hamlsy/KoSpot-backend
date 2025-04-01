package com.kospot.exception.object.domain;

import com.kospot.exception.object.general.GeneralException;
import com.kospot.exception.payload.code.BaseCode;

public class EventHandler extends GeneralException {
    public EventHandler(BaseCode code) {
        super(code);
    }
}
