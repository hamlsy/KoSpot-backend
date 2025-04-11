package com.kospot.exception.object.domain;

import com.kospot.exception.object.general.GeneralException;
import com.kospot.exception.payload.code.BaseCode;

public class GamePlayerHandler extends GeneralException {
    public GamePlayerHandler(BaseCode code) {
        super(code);
    }
}
