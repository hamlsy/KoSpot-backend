package com.kospot.common.exception.object.domain;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class WebSocketHandler extends GeneralException {
    public WebSocketHandler(BaseCode code) {
        super(code);
    }
}
