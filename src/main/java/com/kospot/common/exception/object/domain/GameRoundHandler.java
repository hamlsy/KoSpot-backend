package com.kospot.common.exception.object.domain;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class GameRoundHandler extends GeneralException {
    public GameRoundHandler(BaseCode code) {
        super(code);
    }
}
