package com.kospot.infrastructure.exception.object.domain;

import com.kospot.infrastructure.exception.object.general.GeneralException;
import com.kospot.infrastructure.exception.payload.code.BaseCode;

public class GamePlayerHandler extends GeneralException {
    public GamePlayerHandler(BaseCode code) {
        super(code);
    }
}
