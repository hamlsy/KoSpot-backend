package com.kospot.global.exception.object.domain;

import com.kospot.global.exception.object.general.GeneralException;
import com.kospot.global.exception.payload.code.BaseCode;

public class GameRoundHandler extends GeneralException {
    public GameRoundHandler(BaseCode code) {
        super(code);
    }
}
