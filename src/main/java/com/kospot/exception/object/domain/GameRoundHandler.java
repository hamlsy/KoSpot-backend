package com.kospot.exception.object.domain;

import com.kospot.exception.object.general.GeneralException;
import com.kospot.exception.payload.code.BaseCode;

public class GameRoundHandler extends GeneralException {
    public GameRoundHandler(BaseCode code) {
        super(code);
    }
}
